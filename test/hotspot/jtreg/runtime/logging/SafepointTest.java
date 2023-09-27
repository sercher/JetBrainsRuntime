/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8140348
 * @summary safepoint=trace should have output from each log statement in the code
 * @requires vm.flagless
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver SafepointTest
 */

import java.lang.ref.WeakReference;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class SafepointTest {
    public static void main(String[] args) throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-Xlog:safepoint=trace",
                                                                  InnerClass.class.getName());
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain("Safepoint synchronization initiated");
        output.shouldHaveExitValue(0);
    }

    public static class InnerClass {
        public static byte[] garbage;
        public static volatile WeakReference<Object> weakref;

        public static void createweakref() {
            Object o = new Object();
            weakref = new WeakReference<>(o);
        }

        public static void main(String[] args) throws Exception {
            // Cause several safepoints to run GC to see safepoint messages
            for (int i = 0; i < 2; i++) {
                createweakref();
                while(weakref.get() != null) {
                    garbage = new byte[8192];
                    System.gc();
                }
            }
        }
    }
}
