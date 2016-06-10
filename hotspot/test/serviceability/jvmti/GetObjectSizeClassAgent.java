/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.lang.instrument.*;

public class GetObjectSizeClassAgent {

    static Instrumentation instrumentation;

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        GetObjectSizeClassAgent.instrumentation = instrumentation;
    }

    public static void main(String[] args) throws Exception {
        long sizeA = instrumentation.getObjectSize(A.class);
        long sizeB = instrumentation.getObjectSize(B.class);

        if (sizeA != sizeB) {
            throw new RuntimeException("java.lang.Class sizes disagree: " + sizeA + " vs. " + sizeB);
        }

        System.out.println("GetObjectSizeClass passed");
    }

    static class A {
    }

    static class B {
        void m() {}
    }

}
