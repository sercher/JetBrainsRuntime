/*
 * Copyright (c) 2020, 2023, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Stack guard pages should be installed correctly and removed when thread is detached
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @requires os.family == "linux"
 * @compile DoOverflow.java
 * @run main/native TestStackGuardPages
 */
import jdk.test.lib.Utils;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;


public class TestStackGuardPages {
    public static void main(String args[]) throws Exception {
        ProcessBuilder pb = ProcessTools.createNativeTestProcessBuilder("invoke",
                                                                        "test_java_overflow");
        pb.environment().put("CLASSPATH", Utils.TEST_CLASS_PATH);
        OutputAnalyzer output = ProcessTools.executeProcess(pb);
        output.shouldHaveExitValue(0);

        pb = ProcessTools.createNativeTestProcessBuilder("invoke",
                                                         "test_java_overflow_initial");
        pb.environment().put("CLASSPATH", Utils.TEST_CLASS_PATH);
        output = ProcessTools.executeProcess(pb);
        output.shouldHaveExitValue(0);
    }
}

