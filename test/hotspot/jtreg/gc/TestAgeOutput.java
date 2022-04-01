/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

package gc;

/*
 * @test TestAgeOutput
 * @bug 8164936
 * @summary Check that collectors using age table based aging print an age table even for the first garbage collection
 * @key gc
 * @requires vm.gc=="null"
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -XX:+UseSerialGC gc.TestAgeOutput UseSerialGC
 * @run main/othervm -XX:+UseG1GC gc.TestAgeOutput UseG1GC
 */

/*
 * @test TestAgeOutputCMS
 * @bug 8164936
 * @key gc
 * @comment Graal does not support CMS
 * @requires vm.gc=="null" & !vm.graal.enabled
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -XX:+UseConcMarkSweepGC gc.TestAgeOutput UseConcMarkSweepGC
 */

import sun.hotspot.WhiteBox;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jdk.test.lib.Platform;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

import static jdk.test.lib.Asserts.*;

public class TestAgeOutput {

    public static void checkPattern(String pattern, String what) throws Exception {
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(what);

        if (!m.find()) {
            throw new RuntimeException("Could not find pattern " + pattern + " in output");
        }
    }

    public static void runTest(String gcArg) throws Exception {
        final String[] arguments = {
            "-Xbootclasspath/a:.",
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:+WhiteBoxAPI",
            "-XX:+" + gcArg,
            "-Xmx10M",
            "-Xlog:gc+age=trace",
            GCTest.class.getName()
            };

        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(arguments);
        OutputAnalyzer output = new OutputAnalyzer(pb.start());

        output.shouldHaveExitValue(0);

        System.out.println(output.getStdout());

        String stdout = output.getStdout();

        checkPattern(".*GC\\(0\\) .*Desired survivor size.*", stdout);
        checkPattern(".*GC\\(0\\) .*Age table with threshold.*", stdout);
        checkPattern(".*GC\\(0\\) .*- age   1:.*", stdout);
    }

    public static void main(String[] args) throws Exception {
        runTest(args[0]);
    }

    static class GCTest {
        private static final WhiteBox WB = WhiteBox.getWhiteBox();

        public static Object holder;

        public static void main(String [] args) {
            holder = new byte[100];
            WB.youngGC();
            System.out.println(holder);
        }
    }
}

