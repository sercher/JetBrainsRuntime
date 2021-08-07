/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 6935535
 * @summary String.indexOf() returns incorrect result on x86 with SSE4.2
 *
 * @run main/othervm -Xcomp compiler.codegen.Test6935535
 */

/**
 * @test
 * @bug 8264223
 * @summary add CodeHeap verification
 *
 * @requires vm.debug
 * @run main/othervm -Xcomp -XX:+VerifyCodeCache compiler.codegen.Test6935535
 */
package compiler.codegen;

public class Test6935535 {

    static int IndexOfTest(String str) {
        return str.indexOf("1111111111111xx1x");
    }

    public static void main(String args[]) {
        String str = "1111111111111xx1111111111111xx1x";
        str = str.substring(0, 31);
        int idx = IndexOfTest(str);
        System.out.println("IndexOf(" + "1111111111111xx1x" + ") = " + idx + " in " + str);
        if (idx != -1) {
            System.exit(97);
        }
    }
}

