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

/*
 * @test
 * @bug 6888954
 * @bug 8015884
 * @summary Exercise HotSpot error handling code by invoking java with
 *          -XX:ErrorHandlerTest option to cause an error report. Check the results.
 * @modules java.base/jdk.internal.misc
 * @library /testlibrary
 * @run driver ErrorHandler
 */

import jdk.test.lib.*;

public class ErrorHandler {

    public static OutputAnalyzer runTest(int testcase) throws Exception {
        return new OutputAnalyzer(
            ProcessTools.createJavaProcessBuilder(
            "-XX:-TransmitErrorReport", "-XX:-CreateCoredumpOnCrash", "-XX:ErrorHandlerTest=" + testcase)
            .start());
    }

    public static void main(String[] args) throws Exception {
        // Test is only applicable for debug builds
        if (!Platform.isDebugBuild()) {
            return;
        }
        // Keep this in sync with hotspot/src/share/vm/utilities/debug.cpp
        int i = 1;
        String[] strings = {
            "assert(str == NULL) failed: expected null",
            "assert(num == 1023 && *str == 'X') failed: num=",
            "guarantee(str == NULL) failed: expected null",
            "guarantee(num == 1023 && *str == 'X') failed: num=",
            "fatal error: expected null",
            "fatal error: num=",
            "fatal error: this message should be truncated during formatting",
            "ChunkPool::allocate",
            "Error: ShouldNotCall()",
            "Error: ShouldNotReachHere()",
            "Error: Unimplemented()"
        };

        String[] patterns = {
            "(SIGILL|SIGSEGV|EXCEPTION_ACCESS_VIOLATION).* at pc=",
            "(SIGBUS|SIGSEGV|SIGILL|EXCEPTION_ACCESS_VIOLATION).* at pc="
        };

        for (String s : strings) {
            runTest(i++).shouldContain(s);
        }

        for (String p : patterns) {
            runTest(i++).shouldMatch(p);
        }
    }
}
