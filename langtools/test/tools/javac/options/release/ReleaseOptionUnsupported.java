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
 * @bug 8178152
 * @summary Verify unsupported modules and module options handling.
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.jvm
 *          jdk.jdeps/com.sun.tools.classfile
 *          jdk.jdeps/com.sun.tools.javap
 * @build toolbox.ToolBox toolbox.JarTask toolbox.JavacTask toolbox.JavapTask toolbox.TestRunner
 * @run main ReleaseOptionUnsupported
 */

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import com.sun.tools.javac.jvm.Target;
import toolbox.JavacTask;
import toolbox.Task;
import toolbox.Task.Expect;
import toolbox.TestRunner;
import toolbox.ToolBox;

public class ReleaseOptionUnsupported extends TestRunner {

    private final ToolBox tb = new ToolBox();

    public ReleaseOptionUnsupported() {
        super(System.err);
    }

    public static void main(String... args) throws Exception {
        new ReleaseOptionUnsupported().runTests();
    }

    @Test
    public void testUnsafe(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                          "module m { requires jdk.unsupported; }",
                          "package test; public class Test { sun.misc.Unsafe unsafe; } ");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        List<String> log;
        List<String> expected = Arrays.asList(
                "Test.java:1:43: compiler.warn.sun.proprietary: sun.misc.Unsafe",
                "1 warning"
        );

        log = new JavacTask(tb)
                .options("-XDrawDiagnostics")
                .outdir(classes)
                .files(tb.findJavaFiles(src))
                .run(Expect.SUCCESS)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        if (!expected.equals(log)) {
            throw new AssertionError("Unexpected output: " + log);
        }

        log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "--release", Target.DEFAULT.multiReleaseValue())
                .outdir(classes)
                .files(tb.findJavaFiles(src))
                .run(Expect.SUCCESS)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        if (!expected.equals(log)) {
            throw new AssertionError("Unexpected output: " + log);
        }
    }

    @Test
    public void testUnsafeUnnamed(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                          "package test; public class Test { sun.misc.Unsafe unsafe; } ");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        List<String> log;
        List<String> expected = Arrays.asList(
                "Test.java:1:43: compiler.warn.sun.proprietary: sun.misc.Unsafe",
                "1 warning"
        );

        log = new JavacTask(tb)
                .options("-XDrawDiagnostics")
                .outdir(classes)
                .files(tb.findJavaFiles(src))
                .run(Expect.SUCCESS)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        if (!expected.equals(log)) {
            throw new AssertionError("Unexpected output: " + log);
        }

        log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "--release", Target.DEFAULT.multiReleaseValue())
                .outdir(classes)
                .files(tb.findJavaFiles(src))
                .run(Expect.SUCCESS)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        if (!expected.equals(log)) {
            throw new AssertionError("Unexpected output: " + log);
        }
    }

    @Test
    public void testAddExports(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                          "module m { }",
                          "package test; public class Test { jdk.internal.misc.Unsafe unsafe; } ");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "--add-exports", "java.base/jdk.internal.misc=m")
                .outdir(classes)
                .files(tb.findJavaFiles(src))
                .run(Expect.SUCCESS)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        List<String> log;
        List<String> expected;

        log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "--add-exports", "java.base/jdk.internal.misc=m",
                         "--release", Target.DEFAULT.multiReleaseValue())
                .outdir(classes)
                .files(tb.findJavaFiles(src))
                .run(Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        expected = Arrays.asList(
                "- compiler.err.add.exports.with.release: java.base",
                "1 error"
        );

        if (!expected.equals(log)) {
            throw new AssertionError("Unexpected output: " + log);
        }

        //OK to add exports a package of a non-system module:
        tb.writeJavaFiles(src,
                          "package test; public class Test { } ");
        tb.createDirectories(classes);

        new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "--add-exports", "m/test=ALL-UNNAMED",
                         "--release", Target.DEFAULT.multiReleaseValue())
                .outdir(classes)
                .files(tb.findJavaFiles(src))
                .run(Expect.SUCCESS)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);
    }

    @Test
    public void testAddReads(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                          "module m { }",
                          "package test; public class Test { } ");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "--add-reads", "java.base=m")
                .outdir(classes)
                .files(tb.findJavaFiles(src))
                .run(Expect.SUCCESS)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        List<String> log;
        List<String> expected;

        log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "--add-reads", "java.base=m",
                         "--release", Target.DEFAULT.multiReleaseValue())
                .outdir(classes)
                .files(tb.findJavaFiles(src))
                .run(Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        expected = Arrays.asList(
                "- compiler.err.add.reads.with.release: java.base",
                "1 error"
        );

        if (!expected.equals(log)) {
            throw new AssertionError("Unexpected output: " + log);
        }

        //OK to add reads a package of a non-system module:
        tb.createDirectories(classes);

        new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "--add-reads", "m=java.base",
                         "--release", Target.DEFAULT.multiReleaseValue())
                .outdir(classes)
                .files(tb.findJavaFiles(src))
                .run(Expect.SUCCESS)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);
    }

    @Test
    public void testPatchModule(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                          "module m { }",
                          "package test; public class Test { } ");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);
        Path patch = base.resolve("patch");
        tb.createDirectories(patch);

        new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "--patch-module", "java.base=" + patch)
                .outdir(classes)
                .files(tb.findJavaFiles(src))
                .run(Expect.SUCCESS)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        List<String> log;
        List<String> expected;

        log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "--patch-module", "java.base=" + patch,
                         "--release", Target.DEFAULT.multiReleaseValue())
                .outdir(classes)
                .files(tb.findJavaFiles(src))
                .run(Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        expected = Arrays.asList(
                "- compiler.err.patch.module.with.release: java.base",
                "1 error"
        );

        if (!expected.equals(log)) {
            throw new AssertionError("Unexpected output: " + log);
        }

        //OK to patch a non-system module:
        tb.createDirectories(classes);

        new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "--patch-module", "m=" + patch,
                         "--release", Target.DEFAULT.multiReleaseValue())
                .outdir(classes)
                .files(tb.findJavaFiles(src))
                .run(Expect.SUCCESS)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);
    }

    protected void runTests() throws Exception {
        runTests(m -> new Object[] { Paths.get(m.getName()) });
    }
}
