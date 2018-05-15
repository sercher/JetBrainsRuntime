/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.jfr.internal;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.Callable;

/**
 * Purpose of this class is to simplify analysis of security risks.
 * <p>
 * Paths in the public API should be wrapped in this class so we
 * at all time know what kind of paths we are dealing with.
 * <p>
 * A user supplied path must never be used in an unsafe context, such as a
 * shutdown hook or any other thread created by JFR.
 * <p>
 * All operation using this path must happen in {@link #doPriviligedIO(Callable)}
 */
public final class WriteableUserPath {
    private final AccessControlContext controlContext;
    private final Path original;
    private final Path real;
    private final String text;

    // Not to ensure security, but to help
    // against programming errors
    private volatile boolean inPrivileged;

    public WriteableUserPath(Path path) throws IOException {
        controlContext = AccessController.getContext();
        // verify that the path is writeable
        if (Files.exists(path) && !Files.isWritable(path)) {
            // throw same type of exception as FileOutputStream
            // constructor, if file can't be opened.
            throw new FileNotFoundException("Could not write to file: " + path.toAbsolutePath());
        }
        // will throw if non-writeable
        BufferedWriter fw = Files.newBufferedWriter(path);
        fw.close();
        this.original = path;
        this.real = path.toRealPath();
        this.text = real.toString();
    }

    /**
     * Returns a potentially malicious path where the user may have implemented
     * their own version of Path. This method should never be called in an
     * unsafe context and the Path value should never be passed along to other
     * methods.
     *
     * @return path from a potentially malicious user
     */
    public Path getPotentiallyMaliciousOriginal() {
        return original;
    }

    /**
     * Returns a string representation of the path.
     *
     * @return path as text
     */
    public String getText() {
        return text;
    }

    /**
     * Returns a potentially malicious path where the user may have implemented
     * their own version of Path. This method should never be called in an
     * unsafe context and the Path value should never be passed along to other
     * methods.
     *
     * @return path from a potentially malicious user
     */
    public Path getReal() {
        if (!inPrivileged) {
            throw new InternalError("A user path was accessed outside the context it was supplied in");
        }
        return real;
    }

    public void doPriviligedIO(Callable<?> function) throws IOException {
        try {
            inPrivileged = true;
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                @Override
                public Void run() throws Exception {
                    function.call();
                    return null;
                }
            }, controlContext);
        } catch (Throwable t) {
            // prevent malicious user to propagate exception callback
            // in the wrong context
            throw new IOException("Unexpected error during I/O operation");
        } finally {
            inPrivileged = false;
        }
    }
}
