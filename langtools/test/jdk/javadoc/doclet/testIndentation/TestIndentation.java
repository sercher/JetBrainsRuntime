/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      8011288 8062647 8175200
 * @summary  Erratic/inconsistent indentation of signatures
 * @library  ../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    JavadocTester
 * @run main TestIndentation
 */

public class TestIndentation extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestIndentation tester = new TestIndentation();
        tester.runTests();
    }

    @Test
    void test() {
        javadoc("-d", "out",
                "-sourcepath", testSrc,
                "p");
        checkExit(Exit.OK);

        checkOutput("p/Indent.html", true,
                "<pre>public&nbsp;&lt;T&gt;&nbsp;void&nbsp;m&#8203;(T&nbsp;t1,",
                "\n"
                + "                  T&nbsp;t2)",
                "\n"
                + "           throws java.lang.Exception");

        // Test indentation of annotations and annotated method arguments
        checkOutput("p/IndentAnnot.html", false,
                " @Deprecated",
                "                int&nbsp;b)",
                "                java.lang.Object...&nbsp;b)");

    }
}
