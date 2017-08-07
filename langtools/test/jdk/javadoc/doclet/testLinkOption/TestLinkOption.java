/*
 * Copyright (c) 2002, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4720957 5020118 8026567 8038976 8184969
 * @summary Test to make sure that -link and -linkoffline link to
 * right files, and URLs with and without trailing slash are accepted.
 * @author jamieh
 * @library ../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build JavadocTester
 * @run main TestLinkOption
 */

import java.io.File;

public class TestLinkOption extends JavadocTester {
    /**
     * The entry point of the test.
     * @param args the array of command line arguments.
     */
    public static void main(String... args) throws Exception {
        TestLinkOption tester = new TestLinkOption();
        tester.runTests();
    }

    // The following test runs javadoc multiple times; it is important that the
    // first one is run first, since the subsequent runs refer to the output
    // it generates. Therefore we run everything serially in a single @Test
    // method and not in independent @Test methods.
    @Test
    void test() {
        String mylib = "mylib";
        String[] javacArgs = {
            "-d", mylib, testSrc + "/extra/StringBuilder.java"
        };
        com.sun.tools.javac.Main.compile(javacArgs);

        // Generate the documentation using -linkoffline and a URL as the first parameter.
        String out1 = "out1";
        String url = "http://acme.com/jdk/";
        javadoc("-d", out1,
                "-classpath", mylib,
                "-sourcepath", testSrc,
                "-linkoffline", url, testSrc + "/jdk",
                "-package",
                "pkg", "mylib.lang");
        checkExit(Exit.OK);

        checkOutput("pkg/C.html", true,
                "<a href=\"" + url + "java/lang/String.html?is-external=true\" "
                + "title=\"class or interface in java.lang\"><code>Link to String Class</code></a>",
                //Make sure the parameters are indented properly when the -link option is used.
                "(int&nbsp;p1,\n"
                + "      int&nbsp;p2,\n"
                + "      int&nbsp;p3)",
                "(int&nbsp;p1,\n"
                + "      int&nbsp;p2,\n"
                + "      <a href=\"" + url + "java/lang/Object.html?is-external=true\" title=\"class or interface in java.lang\">"
                + "Object</a>&nbsp;p3)");

        checkOutput("pkg/B.html", true,
                "<div class=\"block\">A method with html tag the method "
                + "<a href=\"" + url + "java/lang/ClassLoader.html?is-external=true#getSystemClassLoader--\""
                + " title=\"class or interface in java.lang\"><code><tt>getSystemClassLoader()</tt>"
                + "</code></a> as the parent class loader.</div>",
                "<div class=\"block\">is equivalent to invoking <code>"
                + "<a href=\"../pkg/B.html#createTempFile-java.lang.String-java.lang.String-java.io.File-\">"
                + "<code>createTempFile(prefix,&nbsp;suffix,&nbsp;null)</code></a></code>.</div>",
                "<a href=\"" + url + "java/lang/String.html?is-external=true\" "
                + "title=\"class or interface in java.lang\">Link-Plain to String Class</a>",
                "<code><tt>getSystemClassLoader()</tt></code>",
                "<code>createTempFile(prefix,&nbsp;suffix,&nbsp;null)</code>",
                "<dd><a href=\"http://www.ietf.org/rfc/rfc2279.txt\"><i>RFC&nbsp;2279: UTF-8, a\n" +
                " transformation format of ISO 10646</i></a>, <br><a " +
                "href=\"http://www.ietf.org/rfc/rfc2373.txt\"><i>RFC&nbsp;2373: IPv6 Addressing\n" +
                " Architecture</i></a>, <br><a href=\"http://www.ietf.org/rfc/rfc2396.txt\">" +
                "<i>RFC&nbsp;2396: Uniform\n" +
                " Resource Identifiers (URI): Generic Syntax</i></a>, " +
                "<br><a href=\"http://www.ietf.org/rfc/rfc2732.txt\"><i>RFC&nbsp;2732: Format for\n" +
                " Literal IPv6 Addresses in URLs</i></a>, <br><a href=\"URISyntaxException.html\">" +
                "URISyntaxException</a></dd>\n" +
                "</dl>");

        checkOutput("mylib/lang/StringBuilderChild.html", true,
                "<pre>public abstract class <span class=\"typeNameLabel\">StringBuilderChild</span>\n"
                + "extends <a href=\"" + url + "java/lang/Object.html?is-external=true\" "
                + "title=\"class or interface in java.lang\">Object</a></pre>"
        );

        // Generate the documentation using -linkoffline and a relative path as the first parameter.
        // We will try linking to the docs generated in test 1 with a relative path.
        String out2 = "out2";
        javadoc("-d", out2,
                "-sourcepath", testSrc,
                "-linkoffline", "../" + out1, out1,
                "-package",
                "pkg2");
        checkExit(Exit.OK);
        checkOutput("pkg2/C2.html", true,
            "This is a link to <a href=\"../../" + out1 + "/pkg/C.html?is-external=true\" " +
            "title=\"class or interface in pkg\"><code>Class C</code></a>."
        );

        String out3 = "out3";
        javadoc(createArguments(out3, out1, true));  // with trailing slash
        checkExit(Exit.OK);

        String out4 = "out4";
        javadoc(createArguments(out4, out1, false)); // without trailing slash
        checkExit(Exit.OK);
        // Note: the following test is very weak, and will fail if ever the text
        // of the message is changed. We should have a separate test to verify
        // this is the text that is given when there is a problem with a URL
        checkOutput(Output.OUT, false,
                "warning - Error fetching URL");

        // check multiple link options
        javadoc("-d", "out5",
                "-sourcepath", testSrc,
                "-link", "../" + "out1",
                "-link", "../" + "out2",
                "pkg3");
        checkExit(Exit.OK);
        checkOutput("pkg3/A.html", true,
                "<pre>public class <span class=\"typeNameLabel\">A</span>\n"
                + "extends java.lang.Object</pre>\n"
                + "<div class=\"block\">Test links.\n"
                + " <br>\n"
                + " <a href=\"../../out2/pkg2/C2.html?is-external=true\" "
                + "title=\"class or interface in pkg2\"><code>link to pkg2.C2</code></a>\n"
                + " <br>\n"
                + " <a href=\"../../out1/mylib/lang/StringBuilderChild.html?is-external=true\" "
                + "title=\"class or interface in mylib.lang\">"
                + "<code>link to mylib.lang.StringBuilderChild</code></a>.</div>\n"
        );

        // check multiple linkoffline options
        javadoc("-d", "out6",
                "-sourcepath", testSrc,
                "-linkoffline", "../copy/out1", "out1",
                "-linkoffline", "../copy/out2", "out2",
                "pkg3");
        checkExit(Exit.OK);
        checkOutput("pkg3/A.html", true,
                "<pre>public class <span class=\"typeNameLabel\">A</span>\n"
                        + "extends java.lang.Object</pre>\n"
                        + "<div class=\"block\">Test links.\n"
                        + " <br>\n"
                        + " <a href=\"../../copy/out2/pkg2/C2.html?is-external=true\" "
                        + "title=\"class or interface in pkg2\"><code>link to pkg2.C2</code></a>\n"
                        + " <br>\n"
                        + " <a href=\"../../copy/out1/mylib/lang/StringBuilderChild.html?is-external=true\" "
                        + "title=\"class or interface in mylib.lang\">"
                        + "<code>link to mylib.lang.StringBuilderChild</code></a>.</div>\n"
        );
    }

    /*
     * Create the documentation using the -link option, vary the behavior with
     * both trailing and no trailing slash. We are only interested in ensuring
     * that the command executes with no errors or related warnings.
     */
    static String[] createArguments(String outDir, String packageDir, boolean withTrailingSlash) {
        String packagePath = new File(packageDir).getAbsolutePath();
        if (withTrailingSlash) {
            // add the trailing slash, if it is not present!
            if (!packagePath.endsWith(FS)) {
                packagePath = packagePath + FS;
            }
        } else {
            // remove the trailing slash, if it is present!
            if (packagePath.endsWith(FS)) {
                packagePath = packagePath.substring(0, packagePath.length() - 1);
            }
        }
        String args[] = {
            "-d", outDir,
            "-sourcepath", testSrc,
            "-link", "file:///" + packagePath,
            "-package",
            "pkg2"
        };
        System.out.println("packagePath: " + packagePath);
        return args;
    }
}
