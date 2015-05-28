/*
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 4041231
 * @summary Test UUEncoder.java for proper masking in encodeAtom
 * @modules java.base/sun.misc
 */

import sun.misc.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class Encode {

    public static void main(String[] args) throws Exception {
        UUEncoder encoder = new UUEncoder("encode.buf");
        byte[] buffer = new byte[3];

        buffer[0] = -1;
        buffer[1] = -1;
        buffer[2] = -1;

        ByteArrayInputStream in = new ByteArrayInputStream(buffer);
        ByteArrayOutputStream out = new ByteArrayOutputStream(10);

        encoder.encodeBuffer(in, out);
        byte[] result = out.toByteArray();

        if (result[22] == 31)
            throw new RuntimeException("UUEncoder generates incorrect byte sequences in encodeAtom.");

    }
}
