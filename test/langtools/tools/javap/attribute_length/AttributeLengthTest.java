/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8230918
 * @summary j.l.NASE in javap
 * @modules jdk.jdeps/com.sun.tools.javap
 * @compile JavapBug.jcod
 * @run main AttributeLengthTest
 */

import java.io.*;


import java.util.*;
import java.util.Properties;

public class AttributeLengthTest {
    public static void main(String... args) throws Exception {
        String testClasses = System.getProperty("test.classes");
        String fileSep = System.getProperty("file.separator");

        String[] opts = { "-v", testClasses + fileSep + "JavapBug.class" };
        StringWriter sw = new StringWriter();
        PrintWriter pout = new PrintWriter(sw);

        com.sun.tools.javap.Main.run(opts, pout);
        pout.flush();
        if (sw.getBuffer().indexOf("Error: Fatal error: attribute Code too big to handle") == -1) {
            throw new Exception("unexpected javap output");
        }
    }
}
