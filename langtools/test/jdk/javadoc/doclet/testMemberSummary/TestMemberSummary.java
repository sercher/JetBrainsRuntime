/*
 * Copyright (c) 2003, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      4951228 6290760 8025633 8026567 8081854 8162363 8175200 8177417
 * @summary  Test the case where the overriden method returns a different
 *           type than the method in the child class.  Make sure the
 *           documentation is inherited but the return type isn't.
 * @author   jamieh
 * @library  ../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    JavadocTester
 * @run main TestMemberSummary
 */

public class TestMemberSummary extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestMemberSummary tester = new TestMemberSummary();
        tester.runTests();
    }

    @Test
    void test() {
        javadoc("-d", "out", "-private",
                "-sourcepath", testSrc,
                "pkg","pkg2");
        checkExit(Exit.OK);

        checkOutput("pkg/PublicChild.html", true,
                // Check return type in member summary.
                "<code><a href=\"../pkg/PublicChild.html\" title=\"class in pkg\">PublicChild</a></code></td>\n"
                + "<th class=\"colSecond\" scope=\"row\"><code><span class=\"memberNameLink\"><a href=\"../pkg/PublicChild.html#returnTypeTest--\">"
                + "returnTypeTest</a></span>&#8203;()</code>",
                // Check return type in member detail.
                "<pre>public&nbsp;<a href=\"../pkg/PublicChild.html\" title=\"class in pkg\">"
                + "PublicChild</a>&nbsp;returnTypeTest&#8203;()</pre>",
                "<th class=\"colConstructorName\" scope=\"row\"><code><span class=\"memberNameLink\">"
                + "<a href=\"../pkg/PublicChild.html#PublicChild--\">PublicChild</a></span>&#8203;()</code></th>");

        checkOutput("pkg/PrivateParent.html", true,
                "<td class=\"colFirst\"><code>private </code></td>\n"
                + "<th class=\"colConstructorName\" scope=\"row\"><code><span class=\"memberNameLink\">"
                + "<a href=\"../pkg/PrivateParent.html#PrivateParent-int-\">PrivateParent</a></span>&#8203;(int&nbsp;i)</code>"
                + "</th>");

        // Legacy anchor dimensions (6290760)
        checkOutput("pkg2/A.html", true,
                "<a name=\"f-java.lang.Object:A-\">\n"
                + "<!--   -->\n"
                + "</a><a name=\"f-T:A-\">\n"
                + "<!--   -->\n"
                + "</a>");
    }
}
