/*
 * Copyright (c) 2016, 2023, Oracle and/or its affiliates. All rights reserved.
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
 * @test MonitorMismatchTest
 * @bug 8150084
 * @requires vm.flagless
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @compile MonitorMismatchHelper.jasm
 * @run driver MonitorMismatchTest
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.Platform;

public class MonitorMismatchTest {

    public static void main(String... args) throws Exception {
        // monitormismatch should turn on.
        ProcessBuilder pb = ProcessTools.createLimitedTestJavaProcessBuilder("-Xcomp",
                                                                             "-XX:+TieredCompilation",
                                                                             "-Xlog:monitormismatch=info",
                                                                             "MonitorMismatchHelper");
        OutputAnalyzer o = new OutputAnalyzer(pb.start());
        o.shouldHaveExitValue(0);
        o.shouldContain("[monitormismatch] Monitor mismatch in method");

        // monitormismatch should turn off.
        pb = ProcessTools.createLimitedTestJavaProcessBuilder("-Xcomp",
                                                              "-XX:+TieredCompilation",
                                                              "-Xlog:monitormismatch=off",
                                                              "MonitorMismatchHelper");
        o = new OutputAnalyzer(pb.start());
        o.shouldHaveExitValue(0);
        o.shouldNotContain("[monitormismatch]");
    };

}
