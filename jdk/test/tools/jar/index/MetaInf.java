/*
 * Copyright 2003 Sun Microsystems, Inc.  All Rights Reserved.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

/*
 * @test
 * @bug 4408526 6854795
 * @summary Index the non-meta files in META-INF, such as META-INF/services.
 */

import java.io.*;
import java.util.Arrays;
import java.util.jar.*;
import sun.tools.jar.Main;
import java.util.zip.ZipFile;

public class MetaInf {

    static String jarName = "a.jar";
    static String INDEX = "META-INF/INDEX.LIST";
    static String SERVICES = "META-INF/services";
    static String contents =
        System.getProperty("test.src") + File.separatorChar + "jarcontents";

    static void run(String ... args) {
        if (! new Main(System.out, System.err, "jar").run(args))
            throw new Error("jar failed: args=" + Arrays.toString(args));
    }

    static void copy(File from, File to) throws IOException {
        FileInputStream in = new FileInputStream(from);
        FileOutputStream out = new FileOutputStream(to);
        try {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1)
                out.write(buf, 0, n);
        } finally {
            in.close();
            out.close();
        }
    }

    static boolean contains(File jarFile, String entryName)
        throws IOException {
        return new ZipFile(jarFile).getEntry(entryName) != null;
    }

    static void checkContains(File jarFile, String entryName)
        throws IOException {
        if (! contains(jarFile, entryName))
            throw new Error(String.format("expected jar %s to contain %s",
                                          jarFile, entryName));
    }

    static void testIndex(String jarName) throws IOException {
        System.err.printf("jarName=%s%n", jarName);

        File jar = new File(jarName);

        // Create a jar to be indexed.
        run("cf", jarName, "-C", contents, SERVICES);

        for (int i = 0; i < 2; i++) {
            run("i", jarName);
            checkContains(jar, INDEX);
            checkContains(jar, SERVICES);
        }

        JarFile f = new JarFile(jarName);
        BufferedReader index =
            new BufferedReader(
                    new InputStreamReader(
                            f.getInputStream(f.getJarEntry(INDEX))));
        String line;
        while ((line = index.readLine()) != null) {
            if (line.equals(SERVICES)) {
                return;
            }
        }
        throw new Error(SERVICES + " not indexed.");
    }

    public static void main(String[] args) throws IOException {
        testIndex("a.jar");             // a path with parent == null
        testIndex("./a.zip");           // a path with parent != null

        // Try indexing a jar in the default temp directory.
        File tmpFile = File.createTempFile("MetaInf", null, null);
        try {
            testIndex(tmpFile.getPath());
        } finally {
            tmpFile.delete();
        }
    }
}
