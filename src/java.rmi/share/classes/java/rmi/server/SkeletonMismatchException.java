/*
 * Copyright (c) 1996, 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.rmi.server;

import java.rmi.RemoteException;

/**
 * This exception is thrown when a call is received that does not
 * match the available skeleton.  It indicates either that the
 * remote method names or signatures in this interface have changed or
 * that the stub class used to make the call and the skeleton
 * receiving the call were not generated by the same version of
 * the stub protocol.
 *
 * @author  Roger Riggs
 * @since   1.1
 * @deprecated no replacement.  Skeletons are no longer required for remote
 * method calls in the Java 2 platform v1.2 and greater.
 */
@Deprecated
public class SkeletonMismatchException extends RemoteException {

    /* indicate compatibility with JDK 1.1.x version of class */
    private static final long serialVersionUID = -7780460454818859281L;

    /**
     * Constructs a new <code>SkeletonMismatchException</code> with
     * a specified detail message.
     *
     * @param s the detail message
     * @since 1.1
     * @deprecated no replacement
     */
    @Deprecated
    public SkeletonMismatchException(String s) {
        super(s);
    }

}
