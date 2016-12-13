/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @library / /test/lib /testlibrary
 * @modules java.base/jdk.internal.misc
 * @requires vm.bits == "64" & os.arch == "amd64" & os.family == "linux"
 * @build compiler.aot.cli.jaotc.ListOptionWrongFileTest
 * @run driver ClassFileInstaller compiler.aot.cli.jaotc.data.HelloWorldOne
 * @run driver compiler.aot.cli.jaotc.ListOptionWrongFileTest
 * @summary check jaotc can handle incorrect --compile-commands file
 */

package compiler.aot.cli.jaotc;

import compiler.aot.cli.jaotc.data.HelloWorldOne;
import java.io.File;
import jdk.test.lib.Asserts;
import jdk.test.lib.process.OutputAnalyzer;

public class ListOptionWrongFileTest {
    private static final String TESTED_CLASS_NAME = HelloWorldOne.class.getName();
    private static final String[] EXPECTED = new String[]{
        JaotcTestHelper.DEFAULT_LIBRARY_LOAD_MESSAGE,
        TESTED_CLASS_NAME
    };

    private static final String COMPILE_ITEM
            = JaotcTestHelper.getClassAotCompilationName(HelloWorldOne.class);

    public static void main(String[] args) {
        // expecting wrong file to be read but no compilation directive recognized, so, all compiled
        OutputAnalyzer oa = JaotcTestHelper.compileLibrary("--compile-commands", COMPILE_ITEM, COMPILE_ITEM);
        oa.shouldHaveExitValue(0);
        File compiledLibrary = new File(JaotcTestHelper.DEFAULT_LIB_PATH);
        Asserts.assertTrue(compiledLibrary.exists(), "Expecte compiler library to exist");
        JaotcTestHelper.checkLibraryUsage(HelloWorldOne.class.getName(), EXPECTED, null);
    }
}
