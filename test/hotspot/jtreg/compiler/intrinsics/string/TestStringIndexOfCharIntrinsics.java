/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8301491
 * @summary Check for correct return value when calling indexOfChar intrinsics with negative value.
 * @library /test/lib
 *
 * @run main/othervm -XX:CompileCommand=quiet
 *                   -XX:-TieredCompilation
 *                   -XX:CompileCommand=compileonly,compiler.intrinsics.string.TestStringIndexOfCharIntrinsics::testIndexOfChar*
 *                   -XX:CompileCommand=inline,java.lang.String*::indexOf*
 *                   -XX:PerBytecodeTrapLimit=20000
 *                   -XX:PerMethodTrapLimit=20000
 *                   compiler.intrinsics.string.TestStringIndexOfCharIntrinsics
 */

package compiler.intrinsics.string;

import jdk.test.lib.Asserts;

public class TestStringIndexOfCharIntrinsics {

    static byte byArr[] = new byte[500];

    public static void main(String[] args) {
        for (int j = 0; j < byArr.length; j++) {
            byArr[j] = (byte)j;
        }
        // Test value for aarch64
        byArr[24] = 0x7;
        byArr[23] = -0x80;
        // Warmup
        for (int i = 0; i < 10000; i++) {
            testIndexOfCharArg(i);
            testIndexOfCharConst();
        }
        Asserts.assertEquals(testIndexOfCharConst() , -1, "must be -1 (character not found)");
        Asserts.assertEquals(testIndexOfCharArg(-2147483641) , -1, "must be -1 (character not found)");
    }

    static int testIndexOfCharConst() {
        String s = new String(byArr);
        return s.indexOf(-2147483641);
    }

    static int testIndexOfCharArg(int ch) {
        String s = new String(byArr);
        return s.indexOf(ch);
    }
}
