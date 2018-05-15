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

package jdk.management.jfr;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

final class Stream implements Closeable {

    private final long identifier;
    private final BufferedInputStream inputStream;
    private final byte[] buffer;

    private volatile long time;

    Stream(InputStream is, long identifier, int blockSize) {
        this.inputStream = new BufferedInputStream(is, 50000);
        this.identifier = identifier;
        this.buffer = new byte[blockSize];
    }

    private void touch() {
        time = System.currentTimeMillis();
    }

    public long getLastTouched() {
        return time;
    }

    public byte[] read() throws IOException {
        // OK to reuse buffer since this
        // is only used for serialization
        touch();
        int read = inputStream.read(buffer);
        if (read == -1) {
            // null indicate no more data
            return null;
        }
        if (read != buffer.length) {
            byte[] smallerBuffer = new byte[read];
            System.arraycopy(buffer, 0, smallerBuffer, 0, read);
            return smallerBuffer;
        }

        return buffer;
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }

    public long getId() {
        return identifier;
    }
}
