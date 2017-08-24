/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @modules java.xml
 * @library /lib/testlibrary
 * @build ValidateModulesTest JarUtils jdk.testlibrary.*
 * @run testng ValidateModulesTest
 * @summary Basic test for java --validate-modules
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import jdk.testlibrary.ProcessTools;
import jdk.testlibrary.OutputAnalyzer;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

@Test
public class ValidateModulesTest {

    /**
     * Test that the system modules validate.
     */
    public void testSystemModules() throws Exception {
        run("--validate-modules")
                .stdoutShouldContain("java.base")
                .stdoutShouldContain("java.xml")
                .shouldHaveExitValue(0);
    }

    /**
     * Test an automatic module on the module path with classes in the same
     * package as a system module.
     */
    public void testPackageConflict() throws Exception {
        Path tmpdir = Files.createTempDirectory("tmp");

        Path classes = Files.createDirectory(tmpdir.resolve("classes"));
        touch(classes, "javax/xml/XMLConstants.class");
        touch(classes, "javax/xml/parsers/SAXParser.class");

        Path lib = Files.createDirectory(tmpdir.resolve("lib"));
        JarUtils.createJarFile(lib.resolve("xml.jar"), classes);

        int exitValue = run("-p", lib.toString(), "--validate-modules")
                .shouldContain("xml automatic")
                .shouldContain("conflicts with module java.xml")
                .getExitValue();
        assertTrue(exitValue != 0);

    }

    /**
     * Test two modules with the same name in a directory.
     */
    public void testDuplicateModule() throws Exception {
        Path tmpdir = Files.createTempDirectory("tmp");

        Path classes = Files.createDirectory(tmpdir.resolve("classes"));
        touch(classes, "org/foo/Bar.class");

        Path lib = Files.createDirectory(tmpdir.resolve("lib"));
        JarUtils.createJarFile(lib.resolve("foo-1.0.jar"), classes);
        JarUtils.createJarFile(lib.resolve("foo-2.0.jar"), classes);

        int exitValue = run("-p", lib.toString(), "--validate-modules")
                .shouldContain("contains same module")
                .getExitValue();
        assertTrue(exitValue != 0);
    }

    /**
     * Test two modules with the same name in different directories.
     */
    public void testShadowed() throws Exception {
        Path tmpdir = Files.createTempDirectory("tmp");

        Path classes = Files.createDirectory(tmpdir.resolve("classes"));
        touch(classes, "org/foo/Bar.class");

        Path lib1 = Files.createDirectory(tmpdir.resolve("lib1"));
        JarUtils.createJarFile(lib1.resolve("foo-1.0.jar"), classes);

        Path lib2 = Files.createDirectory(tmpdir.resolve("lib2"));
        JarUtils.createJarFile(lib2.resolve("foo-2.0.jar"), classes);

        run("-p", lib1 + File.pathSeparator + lib2, "--validate-modules")
                .shouldContain("shadowed by")
                .shouldHaveExitValue(0);
    }

    /**
     * Runs the java launcher with the given arguments.
     */
    private OutputAnalyzer run(String... args) throws Exception {
        return ProcessTools.executeTestJava(args)
                .outputTo(System.out)
                .errorTo(System.out);
    }

    /**
     * Creates a file relative the given directory.
     */
    private void touch(Path dir, String relPath) throws IOException {
        Path file = dir.resolve(relPath.replace('/', File.separatorChar));
        Files.createDirectories(file.getParent());
        Files.createFile(file);
    }
}
