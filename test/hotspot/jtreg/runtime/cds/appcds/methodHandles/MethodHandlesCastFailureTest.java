/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
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
 *
 */
// this file is auto-generated by ./CDSMHTest_generate.sh. Do not edit manually.

/*
 * @test
 * @summary Run the MethodHandlesCastFailureTest.java test in static CDS archive mode.
 * @requires vm.cds & vm.compMode != "Xcomp"
 * @comment Some of the tests run excessively slowly with -Xcomp. The original
 *          tests aren't executed with -Xcomp in the CI pipeline, so let's exclude
 *          the generated tests from -Xcomp execution as well.
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 *          /test/hotspot/jtreg/runtime/cds/appcds/dynamicArchive/test-classes
 * @compile ../../../../../../jdk/java/lang/invoke/MethodHandlesTest.java
 *        ../../../../../../lib/jdk/test/lib/Utils.java
 *        ../../../../../../jdk/java/lang/invoke/MethodHandlesCastFailureTest.java
 *        ../../../../../../jdk/java/lang/invoke/remote/RemoteExample.java
 *        ../../../../../../jdk/java/lang/invoke/common/test/java/lang/invoke/lib/CodeCacheOverflowProcessor.java
 *        ../dynamicArchive/test-classes/TestMHApp.java
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run junit/othervm/timeout=480 -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:. MethodHandlesCastFailureTest
 */

import org.junit.Test;

import java.io.File;

import jdk.test.lib.cds.CDSOptions;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.Platform;

public class MethodHandlesCastFailureTest {
    @Test
    public void test() throws Exception {
        testImpl();
    }

    private static final String classDir = System.getProperty("test.classes");
    private static final String mainClass = "TestMHApp";
    private static final String javaClassPath = System.getProperty("java.class.path");
    private static final String ps = System.getProperty("path.separator");
    private static final String testPackageName = "test.java.lang.invoke";
    private static final String testClassName = "MethodHandlesCastFailureTest";

    static void testImpl() throws Exception {
        String appJar = JarBuilder.build("MH", new File(classDir), null);
        String classList = testClassName + ".list";
        String archiveName = testClassName + ".jsa";
        // Disable VerifyDpendencies when running with debug build because
        // the test requires a lot more time to execute with the option enabled.
        String verifyOpt =
            Platform.isDebugBuild() ? "-XX:-VerifyDependencies" : "-showversion";

        String junitJar = Path.of(Test.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();

        String jars = appJar + ps + junitJar;

        // dump class list
        CDSTestUtils.dumpClassList(classList, "-cp", jars, verifyOpt, mainClass,
                                   testPackageName + "." + testClassName);

        // create archive with the class list
        CDSOptions opts = (new CDSOptions())
            .addPrefix("-XX:ExtraSharedClassListFile=" + classList,
                       "-cp", jars,
                       "-Xlog:class+load,cds")
            .setArchiveName(archiveName);
        CDSTestUtils.createArchiveAndCheck(opts);

        // run with archive
        CDSOptions runOpts = (new CDSOptions())
            .addPrefix("-cp", jars, "-Xlog:class+load,cds=debug", verifyOpt)
            .setArchiveName(archiveName)
            .setUseVersion(false)
            .addSuffix(mainClass, testPackageName + "." + testClassName);
        OutputAnalyzer output = CDSTestUtils.runWithArchive(runOpts);
        output.shouldMatch(".class.load. test.java.lang.invoke.MethodHandlesCastFailureTest[$][$]Lambda[$].*/0x.*source:.*shared.*objects.*file")
              .shouldHaveExitValue(0);
    }
}
