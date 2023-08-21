/*
 * Copyright (c) 2019, 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.file.Path;
import java.util.List;

import jdk.jpackage.test.JPackageCommand;
import jdk.jpackage.test.TKit;
import jdk.jpackage.test.Executor;
import jdk.jpackage.test.Executor.Result;

public class SigningBase {

    public static String DEV_NAME;
    public static String APP_CERT;
    public static String INSTALLER_CERT;
    public static String KEYCHAIN;
    static {
        String value = System.getProperty("jpackage.mac.signing.key.user.name");
        DEV_NAME = (value == null) ?  "jpackage.openjdk.java.net" : value;
        APP_CERT = "Developer ID Application: " + DEV_NAME;
        INSTALLER_CERT = "Developer ID Installer: " + DEV_NAME;
        value = System.getProperty("jpackage.mac.signing.keychain");
        KEYCHAIN = (value == null) ? "jpackagerTest.keychain" : value;
    }

    // Note: It is not clear if we can combine "--verify" and "--display", so
    // we testing them separately. Since JDK-8298488 unsigned app images are
    // actually signed with adhoc signature and it will pass "--verify", so in
    // addition we will check certificate name which was used to sign.
    private static enum CodesignCheckType {
        VERIFY, // Runs codesign with "--verify" to check signature and 0 exit code
        VERIFY_UNSIGNED, // Runs codesign with "--verify" to check signature and 1 exit code
        DISPLAY // Runs codesign with "--display --verbose=4" to get info about signature
    };

    private static void checkString(List<String> result, String lookupString) {
        TKit.assertTextStream(lookupString).predicate(
                (line, what) -> line.trim().contains(what)).apply(result.stream());
    }

    private static List<String> codesignResult(Path target, CodesignCheckType type) {
        int exitCode = 0;
        Executor executor = new Executor().setExecutable("/usr/bin/codesign");
        switch (type) {
            case CodesignCheckType.VERIFY_UNSIGNED:
                exitCode = 1;
            case CodesignCheckType.VERIFY:
                executor.addArguments("--verify", "--deep", "--strict",
                                      "--verbose=2", target.toString());
                break;
            case CodesignCheckType.DISPLAY:
                executor.addArguments("--display", "--verbose=4", target.toString());
                break;
            default:
                TKit.error("Unknown CodesignCheckType: " + type);
                break;
        }
        return executor.saveOutput().execute(exitCode).getOutput();
    }

    private static void verifyCodesignResult(List<String> result, Path target,
            boolean signed, CodesignCheckType type) {
        result.stream().forEachOrdered(TKit::trace);
        String lookupString;
        switch (type) {
            case CodesignCheckType.VERIFY:
                lookupString = target.toString() + ": valid on disk";
                checkString(result, lookupString);
                lookupString = target.toString() + ": satisfies its Designated Requirement";
                checkString(result, lookupString);
                break;
            case CodesignCheckType.VERIFY_UNSIGNED:
                lookupString = target.toString() + ": code object is not signed at all";
                checkString(result, lookupString);
                break;
            case CodesignCheckType.DISPLAY:
                if (signed) {
                    lookupString = "Authority=" + APP_CERT;
                } else {
                    lookupString = "Signature=adhoc";
                }
                checkString(result, lookupString);
                break;
            default:
                TKit.error("Unknown CodesignCheckType: " + type);
                break;
        }
    }

    private static Result spctlResult(Path target, String type) {
        Result result = new Executor()
                .setExecutable("/usr/sbin/spctl")
                .addArguments("-vvv", "--assess", "--type", type,
                        target.toString())
                .saveOutput()
                .executeWithoutExitCodeCheck();

        // allow exit code 3 for not being notarized
        if (result.getExitCode() != 3) {
            result.assertExitCodeIsZero();
        }
        return result;
    }

    private static void verifySpctlResult(List<String> output, Path target,
            String type, int exitCode) {
        output.stream().forEachOrdered(TKit::trace);
        String lookupString;

        if (exitCode == 0) {
            lookupString = target.toString() + ": accepted";
            checkString(output, lookupString);
        } else if (exitCode == 3) {
            // allow failure purely for not being notarized
            lookupString = target.toString() + ": rejected";
            checkString(output, lookupString);
        }

        if (type.equals("install")) {
            lookupString = "origin=" + INSTALLER_CERT;
        } else {
            lookupString = "origin=" + APP_CERT;
        }
        checkString(output, lookupString);
    }

    private static List<String> pkgutilResult(Path target) {
        List<String> result = new Executor()
                .setExecutable("/usr/sbin/pkgutil")
                .addArguments("--check-signature",
                        target.toString())
                .executeAndGetOutput();

        return result;
    }

    private static void verifyPkgutilResult(List<String> result) {
        result.stream().forEachOrdered(TKit::trace);
        String lookupString = "Status: signed by";
        checkString(result, lookupString);
        lookupString = "1. " + INSTALLER_CERT;
        checkString(result, lookupString);
    }

    public static void verifyCodesign(Path target, boolean signed) {
        List<String> result = codesignResult(target, CodesignCheckType.VERIFY);
        verifyCodesignResult(result, target, signed, CodesignCheckType.VERIFY);

        result = codesignResult(target, CodesignCheckType.DISPLAY);
        verifyCodesignResult(result, target, signed, CodesignCheckType.DISPLAY);
    }

    // Since we no longer have unsigned app image, but we need to check
    // DMG which is not adhoc or certificate signed and we cannot use verifyCodesign
    // for this. verifyDMG() is introduced to check that DMG is unsigned.
    // Should not be used to validated anything else.
    public static void verifyDMG(Path target) {
        if (!target.toString().toLowerCase().endsWith(".dmg")) {
            TKit.error("Unexpected target: " + target);
        }

        List<String> result = codesignResult(target, CodesignCheckType.VERIFY_UNSIGNED);
        verifyCodesignResult(result, target, false, CodesignCheckType.VERIFY_UNSIGNED);
    }

    public static void verifySpctl(Path target, String type) {
        Result result = spctlResult(target, type);
        List<String> output = result.getOutput();

        verifySpctlResult(output, target, type, result.getExitCode());
    }

    public static void verifyPkgutil(Path target) {
        List<String> result = pkgutilResult(target);
        verifyPkgutilResult(result);
    }

    public static void verifyAppImageSignature(JPackageCommand appImageCmd,
            boolean isSigned, String... launchers) throws Exception {
        Path launcherPath = appImageCmd.appLauncherPath();
        SigningBase.verifyCodesign(launcherPath, isSigned);

        final List<String> launchersList = List.of(launchers);
        launchersList.forEach(launcher -> {
            Path testALPath = launcherPath.getParent().resolve(launcher);
            SigningBase.verifyCodesign(testALPath, isSigned);
        });

        Path appImage = appImageCmd.outputBundle();
        SigningBase.verifyCodesign(appImage, isSigned);
        if (isSigned) {
            SigningBase.verifySpctl(appImage, "exec");
        }
    }

}
