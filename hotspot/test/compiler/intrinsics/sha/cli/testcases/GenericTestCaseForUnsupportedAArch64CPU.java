/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.ExitCode;
import jdk.test.lib.Platform;
import jdk.test.lib.cli.CommandLineOptionTest;
import jdk.test.lib.cli.predicate.AndPredicate;
import jdk.test.lib.cli.predicate.NotPredicate;

/**
 * Generic test case for SHA-related options targeted to AArch64 CPUs
 * which don't support instruction required by the tested option.
 */
public class GenericTestCaseForUnsupportedAArch64CPU extends
        SHAOptionsBase.TestCase {
    public GenericTestCaseForUnsupportedAArch64CPU(String optionName) {
        super(optionName, new AndPredicate(Platform::isAArch64,
                new NotPredicate(SHAOptionsBase.getPredicateForOption(
                        optionName))));
    }

    @Override
    protected void verifyWarnings() throws Throwable {
        String shouldPassMessage = String.format("JVM startup should pass with"
                + "option '-XX:-%s' without any warnings", optionName);
        //Verify that option could be disabled without any warnings.
        CommandLineOptionTest.verifySameJVMStartup(null, new String[] {
                        SHAOptionsBase.getWarningForUnsupportedCPU(optionName)
                }, shouldPassMessage, shouldPassMessage, ExitCode.OK,
                CommandLineOptionTest.prepareBooleanFlag(optionName, false));

        shouldPassMessage = String.format("JVM should start with '-XX:+"
                + "%s' flag, but output should contain warning.", optionName);
        // Verify that when the tested option is explicitly enabled, then
        // a warning will occur in VM output.
        CommandLineOptionTest.verifySameJVMStartup(new String[] {
                        SHAOptionsBase.getWarningForUnsupportedCPU(optionName)
                }, null, shouldPassMessage, shouldPassMessage, ExitCode.OK,
                CommandLineOptionTest.prepareBooleanFlag(optionName, true));
    }

    @Override
    protected void verifyOptionValues() throws Throwable {
        // Verify that option is disabled by default.
        CommandLineOptionTest.verifyOptionValueForSameVM(optionName, "false",
                String.format("Option '%s' should be disabled by default",
                        optionName));

        // Verify that option is disabled even if it was explicitly enabled
        // using CLI options.
        CommandLineOptionTest.verifyOptionValueForSameVM(optionName, "false",
                String.format("Option '%s' should be off on unsupported "
                        + "AArch64CPU even if set to true directly", optionName),
                CommandLineOptionTest.prepareBooleanFlag(optionName, true));

        // Verify that option is disabled when +UseSHA was passed to JVM.
        CommandLineOptionTest.verifyOptionValueForSameVM(optionName, "false",
                String.format("Option '%s' should be off on unsupported "
                        + "AArch64CPU even if %s flag set to JVM",
                        optionName, CommandLineOptionTest.prepareBooleanFlag(
                            SHAOptionsBase.USE_SHA_OPTION, true)),
                CommandLineOptionTest.prepareBooleanFlag(
                        SHAOptionsBase.USE_SHA_OPTION, true));
    }
}
