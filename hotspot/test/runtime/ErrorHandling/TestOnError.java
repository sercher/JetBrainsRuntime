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
 * @test TestOnError
 * @summary Test using -XX:OnError=<cmd>
 * @modules java.base/jdk.internal.misc
 * @library /testlibrary
 * @build TestOnError
 * @run main TestOnError
 * @bug 8078470
 */

import jdk.test.lib.*;

public class TestOnError {

    public static void main(String[] args) throws Exception {
        if (!Platform.isDebugBuild()) {
            System.out.println("Test requires a non-product build - skipping");
            return;
        }

        String msg = "Test Succeeded";

        // Execute the VM so that a
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
           "-XX:-TransmitErrorReport",
           "-XX:-CreateCoredumpOnCrash",
           "-XX:ErrorHandlerTest=12", // trigger potential SEGV
           "-XX:OnError=echo " + msg,
           TestOnError.class.getName());

        OutputAnalyzer output = new OutputAnalyzer(pb.start());

        /* Actual output will include:
           #
           # -XX:OnError="echo Test Succeeded"
           #   Executing /bin/sh -c "echo Test Succeeded"...
           Test Succeeded

           So we don't want to match on the "# Executing ..." line, and they
           both get written to stdout.
        */
        output.stdoutShouldMatch("^" + msg); // match start of line only
        System.out.println("PASSED");
    }
}
