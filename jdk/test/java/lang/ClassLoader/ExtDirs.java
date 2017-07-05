/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8060206
 * @summary Extension mechanism is removed
 */

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ExtDirs {
    public static void main(String arg[]) throws Exception {
        String value = System.getProperty("java.ext.dirs");
        if (value != null) {
            throw new RuntimeException("java.ext.dirs not removed: " + value);
        }

        fatalError("-Djava.ext.dirs=foo");
    }

    static void fatalError(String... args) throws Exception {
        List<String> commands = new ArrayList<>();
        String java = System.getProperty("java.home") + "/bin/java";
        commands.add(java);
        for (String s : args) {
            commands.add(s);
        }
        String cpath = System.getProperty("test.classes", ".");
        commands.add("-cp");
        commands.add(cpath);
        commands.add("ExtDirs");

        ProcessBuilder processBuilder = new ProcessBuilder(commands);
        final Process process = processBuilder.start();
        BufferedReader errorStream = new BufferedReader(
                new InputStreamReader(process.getErrorStream()));
        BufferedReader outStream = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
        String errorLine;
        StringBuilder errors = new StringBuilder();
        String outLines;
        while ((errorLine = errorStream.readLine()) != null) {
            errors.append(errorLine).append("\n");
        }
        while ((outLines = outStream.readLine()) != null) {
            System.out.println(outLines);
        }
        errorLine = errors.toString();
        System.err.println(errorLine);
        process.waitFor(1000, TimeUnit.MILLISECONDS);
        int exitStatus = process.exitValue();
        if (exitStatus == 0) {
            throw new RuntimeException("Expect fatal error");
        }
        if (!errorLine.contains("Could not create the Java Virtual Machine")) {
            throw new RuntimeException(errorLine);
        }
    }
}
