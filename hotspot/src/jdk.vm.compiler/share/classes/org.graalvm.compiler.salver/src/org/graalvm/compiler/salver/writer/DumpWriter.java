/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
package org.graalvm.compiler.salver.writer;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.nio.ByteBuffer;

public interface DumpWriter extends Closeable, Flushable, AutoCloseable {

    DumpWriter write(byte b) throws IOException;

    DumpWriter write(byte[] arr) throws IOException;

    DumpWriter write(ByteBuffer buf) throws IOException;

    DumpWriter write(CharSequence csq) throws IOException;

    DumpWriter writeChar(char v) throws IOException;

    DumpWriter writeShort(short v) throws IOException;

    DumpWriter writeInt(int v) throws IOException;

    DumpWriter writeLong(long v) throws IOException;

    DumpWriter writeFloat(float v) throws IOException;

    DumpWriter writeDouble(double v) throws IOException;
}
