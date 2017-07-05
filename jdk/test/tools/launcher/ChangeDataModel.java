/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4894330 4810347 6277269
 * @compile -XDignore.symbol.file ChangeDataModel.java
 * @run main ChangeDataModel
 * @summary Verify -d32 and -d64 options are accepted(rejected) on all platforms
 * @author Joseph D. Darcy, ksrini
 */
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ChangeDataModel extends TestHelper {
    private static final File TestJar      = new File("test" + JAR_FILE_EXT);
    private static final String OPT_PREFIX = "ARCH_OPT:";

    public static void main(String... args) throws Exception {
        String[] code = {
            "   public static void main(String argv[]) {",
            "      System.out.println(\"" + OPT_PREFIX + "-d\" + System.getProperty(\"sun.arch.data.model\", \"none\"));",
            "   }",
        };
        createJar(TestJar, code);

        // verify if data model flag for default data model is accepted
        if (is32Bit) {
            checkAcceptance(javaCmd, "-d32");
        } else if (is64Bit) {
            checkAcceptance(javaCmd, "-d64");
        } else {
            throw new Error("unsupported data model");
        }

        // test dual mode systems
        if (isDualMode) {
            // albeit dual mode we may not have the 64 bit components present
            if (dualModePresent()) {
                // 32-bit -> 64-bit
                checkExecCount(javaCmd, "-d64");
                // 64-bit -> 32-bit
                checkExecCount(java64Cmd, "-d32");

                checkAcceptance(javaCmd, "-d64");
                checkAcceptance(java64Cmd, "-d32");
            } else {
                System.out.println("Warning: no 64-bit components found;" +
                    " only one data model tested.");
            }
        } else {
            // Negative tests: ensure that non-dual mode systems reject the
            // complementary (other) data model
            if (is32Bit) {
                checkRejection(javaCmd, "-d64");
            } else if (is64Bit) {
                checkRejection(javaCmd, "-d32");
            } else {
                throw new Error("unsupported data model");
            }
        }
    }

    static void checkExecCount(String cmd, String dmodel) {
        Map<String, String> envMap = new HashMap<>();
        envMap.put(JLDEBUG_KEY, "true");
        TestResult tr = doExec(envMap, javaCmd, "-d64",
                "-jar", TestJar.getAbsolutePath());
        int count = 0;
        for (String x : tr.testOutput) {
            if (x.contains(EXPECTED_MARKER)) {
                count++;
                if (count > 1) {
                    System.out.println(tr);
                    throw new RuntimeException("Maximum exec count of 1 execeeded");
                }
            }
        }
    }

    static void checkAcceptance(String cmd, String dmodel) {
        TestResult tr = doExec(cmd, dmodel, "-jar", TestJar.getAbsolutePath());
        if (!tr.contains(OPT_PREFIX + dmodel)) {
            System.out.println(tr);
            String message = "Data model flag " + dmodel +
                    " not accepted or had improper effect.";
            throw new RuntimeException(message);
        }
    }

    static void checkRejection(String cmd, String dmodel) {
        TestResult tr = doExec(cmd, dmodel, "-jar", TestJar.getAbsolutePath());
        if (tr.contains(OPT_PREFIX + dmodel)) {
            System.out.println(tr);
            String message = "Data model flag " + dmodel + " was accepted.";
            throw new RuntimeException(message);
        }
    }
}
