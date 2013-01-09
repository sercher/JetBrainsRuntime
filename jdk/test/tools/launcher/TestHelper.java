/*
 * Copyright (c) 2008, 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.io.OutputStream;
import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.regex.Pattern;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.Set;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import static java.nio.file.StandardCopyOption.*;
import static java.nio.file.StandardOpenOption.*;

/**
 * This class provides some common utilities for the launcher tests.
 */
public class TestHelper {
    // commonly used jtreg constants
    static final File TEST_CLASSES_DIR;
    static final File TEST_SOURCES_DIR;

    static final String JAVAHOME = System.getProperty("java.home");
    static final String JAVA_BIN;
    static final boolean isSDK = JAVAHOME.endsWith("jre");
    static final String javaCmd;
    static final String javawCmd;
    static final String java64Cmd;
    static final String javacCmd;
    static final String jarCmd;

    static final JavaCompiler compiler;

    static final boolean debug = Boolean.getBoolean("TestHelper.Debug");
    static final boolean isWindows =
            System.getProperty("os.name", "unknown").startsWith("Windows");
    static final boolean isMacOSX =
            System.getProperty("os.name", "unknown").contains("OS X");
    static final boolean is64Bit =
            System.getProperty("sun.arch.data.model").equals("64");
    static final boolean is32Bit =
            System.getProperty("sun.arch.data.model").equals("32");
    static final boolean isSolaris =
            System.getProperty("os.name", "unknown").startsWith("SunOS");
    static final boolean isLinux =
            System.getProperty("os.name", "unknown").startsWith("Linux");
    static final boolean isDualMode = isSolaris;
    static final boolean isSparc = System.getProperty("os.arch").startsWith("sparc");

    // make a note of the golden default locale
    static final Locale DefaultLocale = Locale.getDefault();

    static final String JAVA_FILE_EXT  = ".java";
    static final String CLASS_FILE_EXT = ".class";
    static final String JAR_FILE_EXT   = ".jar";
    static final String EXE_FILE_EXT   = ".exe";
    static final String JLDEBUG_KEY     = "_JAVA_LAUNCHER_DEBUG";
    static final String EXPECTED_MARKER = "TRACER_MARKER:About to EXEC";
    static final String TEST_PREFIX     = "###TestError###: ";

    static int testExitValue = 0;

    static {
        String tmp = System.getProperty("test.classes", null);
        if (tmp == null) {
            throw new Error("property test.classes not defined ??");
        }
        TEST_CLASSES_DIR = new File(tmp).getAbsoluteFile();

        tmp = System.getProperty("test.src", null);
        if (tmp == null) {
            throw new Error("property test.src not defined ??");
        }
        TEST_SOURCES_DIR = new File(tmp).getAbsoluteFile();

        if (is64Bit && is32Bit) {
            throw new RuntimeException("arch model cannot be both 32 and 64 bit");
        }
        if (!is64Bit && !is32Bit) {
            throw new RuntimeException("arch model is not 32 or 64 bit ?");
        }
        compiler = ToolProvider.getSystemJavaCompiler();
        File binDir = (isSDK) ? new File((new File(JAVAHOME)).getParentFile(), "bin")
            : new File(JAVAHOME, "bin");
        JAVA_BIN = binDir.getAbsolutePath();
        File javaCmdFile = (isWindows)
                ? new File(binDir, "java.exe")
                : new File(binDir, "java");
        javaCmd = javaCmdFile.getAbsolutePath();
        if (!javaCmdFile.canExecute()) {
            throw new RuntimeException("java <" + TestHelper.javaCmd +
                    "> must exist and should be executable");
        }

        File javacCmdFile = (isWindows)
                ? new File(binDir, "javac.exe")
                : new File(binDir, "javac");
        javacCmd = javacCmdFile.getAbsolutePath();

        File jarCmdFile = (isWindows)
                ? new File(binDir, "jar.exe")
                : new File(binDir, "jar");
        jarCmd = jarCmdFile.getAbsolutePath();
        if (!jarCmdFile.canExecute()) {
            throw new RuntimeException("java <" + TestHelper.jarCmd +
                    "> must exist and should be executable");
        }

        if (isWindows) {
            File javawCmdFile = new File(binDir, "javaw.exe");
            javawCmd = javawCmdFile.getAbsolutePath();
            if (!javawCmdFile.canExecute()) {
                throw new RuntimeException("java <" + javawCmd +
                        "> must exist and should be executable");
            }
        } else {
            javawCmd = null;
        }

        if (!javacCmdFile.canExecute()) {
            throw new RuntimeException("java <" + javacCmd +
                    "> must exist and should be executable");
        }
        if (isSolaris) {
            File sparc64BinDir = new File(binDir,isSparc ? "sparcv9" : "amd64");
            File java64CmdFile= new File(sparc64BinDir, "java");
            if (java64CmdFile.exists() && java64CmdFile.canExecute()) {
                java64Cmd = java64CmdFile.getAbsolutePath();
            } else {
                java64Cmd = null;
            }
        } else {
            java64Cmd = null;
        }
    }
    void run(String[] args) throws Exception {
        int passed = 0, failed = 0;
        final Pattern p = (args != null && args.length > 0)
                ? Pattern.compile(args[0])
                : null;
        for (Method m : this.getClass().getDeclaredMethods()) {
            boolean selected = (p == null)
                    ? m.isAnnotationPresent(Test.class)
                    : p.matcher(m.getName()).matches();
            if (selected) {
                try {
                    m.invoke(this, (Object[]) null);
                    System.out.println(m.getName() + ": OK");
                    passed++;
                    System.out.printf("Passed: %d, Failed: %d, ExitValue: %d%n",
                                      passed, failed, testExitValue);
                } catch (Throwable ex) {
                    System.out.printf("Test %s failed: %s %n", m, ex.getCause());
                    failed++;
                }
            }
        }
        System.out.printf("Total: Passed: %d, Failed %d%n", passed, failed);
        if (failed > 0) {
            throw new RuntimeException("Tests failed: " + failed);
        }
        if (passed == 0 && failed == 0) {
            throw new AssertionError("No test(s) selected: passed = " +
                    passed + ", failed = " + failed + " ??????????");
        }
    }

    /*
     * is a dual mode available in the test jdk
     */
    static boolean dualModePresent() {
        return isDualMode && java64Cmd != null;
    }

    /*
     * usually the jre/lib/arch-name is the same as os.arch, except for x86.
     */
    static String getJreArch() {
        String arch = System.getProperty("os.arch");
        return arch.equals("x86") ? "i386" : arch;
    }

    /*
     * get the complementary jre arch ie. if sparc then return sparcv9 and
     * vice-versa.
     */
    static String getComplementaryJreArch() {
        String arch = System.getProperty("os.arch");
        if (arch != null) {
            switch (arch) {
                case "sparc":
                    return "sparcv9";
                case "sparcv9":
                    return "sparc";
                case "x86":
                    return "amd64";
                case "amd64":
                    return "i386";
            }
        }
        return null;
    }

    static File getClassFile(File javaFile) {
        String s = javaFile.getAbsolutePath().replace(JAVA_FILE_EXT, CLASS_FILE_EXT);
        return new File(s);
    }

    static File getJavaFile(File classFile) {
        String s = classFile.getAbsolutePath().replace(CLASS_FILE_EXT, JAVA_FILE_EXT);
        return new File(s);
    }

    static String baseName(File f) {
        String s = f.getName();
        return s.substring(0, s.indexOf("."));
    }

    /*
     * A convenience method to create a jar with jar file name and defs
     */
    static void createJar(File jarName, String... mainDefs)
            throws FileNotFoundException{
        createJar(null, jarName, new File("Foo"), mainDefs);
    }

    /*
     * A convenience method to create a java file, compile and jar it up, using
     * the sole class file name in the jar, as the Main-Class attribute value.
     */
    static void createJar(File jarName, File mainClass, String... mainDefs)
            throws FileNotFoundException {
            createJar(null, jarName, mainClass, mainDefs);
    }

    /*
     * A convenience method to compile java files.
     */
    static void compile(String... compilerArgs) {
        if (compiler.run(null, null, null, compilerArgs) != 0) {
            String sarg = "";
            for (String x : compilerArgs) {
                sarg.concat(x + " ");
            }
            throw new Error("compilation failed: " + sarg);
        }
    }

    /*
     * A generic jar file creator to create a java file, compile it
     * and jar it up, a specific Main-Class entry name in the
     * manifest can be specified or a null to use the sole class file name
     * as the Main-Class attribute value.
     */
    static void createJar(String mEntry, File jarName, File mainClass,
            String... mainDefs) throws FileNotFoundException {
        if (jarName.exists()) {
            jarName.delete();
        }
        try (PrintStream ps = new PrintStream(new FileOutputStream(mainClass + ".java"))) {
            ps.println("public class Foo {");
            if (mainDefs != null) {
                for (String x : mainDefs) {
                    ps.println(x);
                }
            }
            ps.println("}");
        }

        String compileArgs[] = {
            mainClass + ".java"
        };
        if (compiler.run(null, null, null, compileArgs) != 0) {
            throw new RuntimeException("compilation failed " + mainClass + ".java");
        }
        if (mEntry == null) {
            mEntry = mainClass.getName();
        }
        String jarArgs[] = {
            (debug) ? "cvfe" : "cfe",
            jarName.getAbsolutePath(),
            mEntry,
            mainClass.getName() + ".class"
        };
        createJar(jarArgs);
    }

   static void createJar(String... args) {
        sun.tools.jar.Main jarTool =
                new sun.tools.jar.Main(System.out, System.err, "JarCreator");
        if (!jarTool.run(args)) {
            String message = "jar creation failed with command:";
            for (String x : args) {
                message = message.concat(" " + x);
            }
            throw new RuntimeException(message);
        }
   }

   static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int n = in.read(buf);
        while (n > 0) {
            out.write(buf, 0, n);
            n = in.read(buf);
        }
    }

   static void copyFile(File src, File dst) throws IOException {
        Path parent = dst.toPath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.copy(src.toPath(), dst.toPath(), COPY_ATTRIBUTES, REPLACE_EXISTING);
    }

    /**
     * Attempt to create a file at the given location. If an IOException
     * occurs then back off for a moment and try again. When a number of
     * attempts fail, give up and throw an exception.
     */
    void createAFile(File aFile, List<String> contents) throws IOException {
        IOException cause = null;
        for (int attempts = 0; attempts < 10; attempts++) {
            try {
                Files.write(aFile.getAbsoluteFile().toPath(), contents,
                    Charset.defaultCharset(), CREATE, TRUNCATE_EXISTING, WRITE);
                if (cause != null) {
                    /*
                     * report attempts and errors that were encountered
                     * for diagnostic purposes
                     */
                    System.err.println("Created batch file " +
                                        aFile + " in " + (attempts + 1) +
                                        " attempts");
                    System.err.println("Errors encountered: " + cause);
                    cause.printStackTrace();
                }
                return;
            } catch (IOException ioe) {
                if (cause != null) {
                    // chain the exceptions so they all get reported for diagnostics
                    cause.addSuppressed(ioe);
                } else {
                    cause = ioe;
                }
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException ie) {
                if (cause != null) {
                    // cause should alway be non-null here
                    ie.addSuppressed(cause);
                }
                throw new RuntimeException("Interrupted while creating batch file", ie);
            }
        }
        throw new RuntimeException("Unable to create batch file", cause);
    }

    static void createFile(File outFile, List<String> content) throws IOException {
        Files.write(outFile.getAbsoluteFile().toPath(), content,
                Charset.defaultCharset(), CREATE_NEW);
    }

    static void recursiveDelete(File target) throws IOException {
        if (!target.exists()) {
            return;
        }
        Files.walkFileTree(target.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                try {
                    Files.deleteIfExists(dir);
                } catch (IOException ex) {
                    System.out.println("Error: could not delete: " + dir.toString());
                    System.out.println(ex.getMessage());
                    return FileVisitResult.TERMINATE;
                }
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                try {
                    Files.deleteIfExists(file);
                } catch (IOException ex) {
                    System.out.println("Error: could not delete: " + file.toString());
                    System.out.println(ex.getMessage());
                    return FileVisitResult.TERMINATE;
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    static TestResult doExec(String...cmds) {
        return doExec(null, null, cmds);
    }

    static TestResult doExec(Map<String, String> envToSet, String...cmds) {
        return doExec(envToSet, null, cmds);
    }
    /*
     * A method which executes a java cmd and returns the results in a container
     */
    static TestResult doExec(Map<String, String> envToSet,
                             Set<String> envToRemove, String...cmds) {
        String cmdStr = "";
        for (String x : cmds) {
            cmdStr = cmdStr.concat(x + " ");
        }
        ProcessBuilder pb = new ProcessBuilder(cmds);
        Map<String, String> env = pb.environment();
        if (envToRemove != null) {
            for (String key : envToRemove) {
                env.remove(key);
            }
        }
        if (envToSet != null) {
            env.putAll(envToSet);
        }
        BufferedReader rdr = null;
        try {
            List<String> outputList = new ArrayList<>();
            pb.redirectErrorStream(true);
            Process p = pb.start();
            rdr = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String in = rdr.readLine();
            while (in != null) {
                outputList.add(in);
                in = rdr.readLine();
            }
            p.waitFor();
            p.destroy();

            return new TestHelper.TestResult(cmdStr, p.exitValue(), outputList,
                    env, new Throwable("current stack of the test"));
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex.getMessage());
        }
    }

    static FileFilter createFilter(final String extension) {
        return new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                String name = pathname.getName();
                if (name.endsWith(extension)) {
                    return true;
                }
                return false;
            }
        };
    }

    static boolean isEnglishLocale() {
        return Locale.getDefault().getLanguage().equals("en");
    }

    /*
     * A class to encapsulate the test results and stuff, with some ease
     * of use methods to check the test results.
     */
    static class TestResult {
        PrintWriter status;
        StringWriter sw;
        int exitValue;
        List<String> testOutput;
        Map<String, String> env;
        Throwable t;
        boolean testStatus;

        public TestResult(String str, int rv, List<String> oList,
                Map<String, String> env, Throwable t) {
            sw = new StringWriter();
            status = new PrintWriter(sw);
            status.println("Executed command: " + str + "\n");
            exitValue = rv;
            testOutput = oList;
            this.env = env;
            this.t = t;
            testStatus = true;
        }

        void appendError(String x) {
            testStatus = false;
            testExitValue++;
            status.println(TEST_PREFIX + x);
        }

        void indentStatus(String x) {
            status.println("  " + x);
        }

        void checkNegative() {
            if (exitValue == 0) {
                appendError("test must not return 0 exit value");
            }
        }

        void checkPositive() {
            if (exitValue != 0) {
                appendError("test did not return 0 exit value");
            }
        }

        boolean isOK() {
            return exitValue == 0;
        }

        boolean isZeroOutput() {
            if (!testOutput.isEmpty()) {
                appendError("No message from cmd please");
                return false;
            }
            return true;
        }

        boolean isNotZeroOutput() {
            if (testOutput.isEmpty()) {
                appendError("Missing message");
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            status.println("++++Begin Test Info++++");
            status.println("Test Status: " + (testStatus ? "PASS" : "FAIL"));
            status.println("++++Test Environment++++");
            for (String x : env.keySet()) {
                indentStatus(x + "=" + env.get(x));
            }
            status.println("++++Test Output++++");
            for (String x : testOutput) {
                indentStatus(x);
            }
            status.println("++++Test Stack Trace++++");
            status.println(t.toString());
            for (StackTraceElement e : t.getStackTrace()) {
                indentStatus(e.toString());
            }
            status.println("++++End of Test Info++++");
            status.flush();
            String out = sw.toString();
            status.close();
            return out;
        }

        boolean contains(String str) {
            for (String x : testOutput) {
                if (x.contains(str)) {
                    return true;
                }
            }
            appendError("string <" + str + "> not found");
            return false;
        }

        boolean notContains(String str) {
             for (String x : testOutput) {
                if (x.contains(str)) {
                    appendError("string <" + str + "> found");
                    return false;
                }
            }
            return true;
        }

        boolean matches(String stringToMatch) {
          for (String x : testOutput) {
                if (x.matches(stringToMatch)) {
                    return true;
                }
            }
            appendError("string <" + stringToMatch + "> not found");
            return false;
        }
    }
    /**
    * Indicates that the annotated method is a test method.
    */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Test {}
}
