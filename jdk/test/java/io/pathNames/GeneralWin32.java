/*
 * Copyright (c) 1998, 2013, Oracle and/or its affiliates. All rights reserved.
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
   @bug 4032066 4039597 4046914 4054511 4065189 4109131 4875229 6983520 8009258
   @summary General exhaustive test of win32 pathname handling
   @author Mark Reinhold

   @build General GeneralWin32
   @run main/timeout=600 GeneralWin32
 */

import java.io.*;

public class GeneralWin32 extends General {


    /**
     * Hardwired UNC pathnames used for testing
     *
     * This test attempts to use the host and share names defined in this class
     * to test UNC pathnames.  The test will not fail if the host or share
     * don't exist, but it will print a warning saying that it was unable to
     * test UNC pathnames completely.
     */
    private static final String EXISTENT_UNC_HOST = "pc-cup01";
    private static final String EXISTENT_UNC_SHARE = "pcdist";
    private static final String NONEXISTENT_UNC_HOST = "non-existent-unc-host";
    private static final String NONEXISTENT_UNC_SHARE = "bogus-share";
    private static final int DEPTH = 2;
    private static String baseDir = null;
    private static String userDir = null;
    private static String relative = null;

    /* Pathnames relative to working directory */

    private static void checkCaseLookup() throws IOException {
        /* Use long names here to avoid 8.3 format, which Samba servers often
           force to lowercase */
        File d1 = new File(relative, "XyZzY0123");
        File d2 = new File(d1, "FOO_bar_BAZ");
        File f = new File(d2, "GLORPified");
        if (!f.exists()) {
            if (!d2.exists()) {
                if (!d2.mkdirs()) {
                    throw new RuntimeException("Can't create directory " + d2);
                }
            }
            OutputStream o = new FileOutputStream(f);
            o.close();
        }
        File f2 = new File(d2.getParent(), "mumble"); /* For later ud tests */
        if (!f2.exists()) {
            OutputStream o = new FileOutputStream(f2);
            o.close();
        }

        /* Computing the canonical path of a Win32 file should expose the true
           case of filenames, rather than just using the input case */
        File y = new File(userDir, f.getPath());
        String ans = y.getPath();
        check(ans, relative + "XyZzY0123\\FOO_bar_BAZ\\GLORPified");
        check(ans, relative + "xyzzy0123\\foo_bar_baz\\glorpified");
        check(ans, relative + "XYZZY0123\\FOO_BAR_BAZ\\GLORPIFIED");
    }

    private static void checkWild(File f) throws Exception {
        try {
            f.getCanonicalPath();
        } catch (IOException x) {
            return;
        }
        throw new Exception("Wildcard path not rejected: " + f);
    }

    private static void checkWildCards() throws Exception {
        File d = new File(baseDir).getCanonicalFile();
        checkWild(new File(d, "*.*"));
        checkWild(new File(d, "*.???"));
        checkWild(new File(new File(d, "*.*"), "foo"));
    }

    private static void checkRelativePaths() throws Exception {
        checkCaseLookup();
        checkWildCards();
        checkNames(3, true, baseDir, relative);
    }


    /* Pathnames with drive specifiers */

    private static char findInactiveDrive() {
        for (char d = 'Z'; d >= 'E'; d--) {
            File df = new File(d + ":\\");
            if (!df.exists()) {
                return d;
            }
        }
        throw new RuntimeException("Can't find an inactive drive");
    }

    private static char findActiveDrive() {
        for (char d = 'C'; d <= 'Z'; d--) {
            File df = new File(d + ":\\");
            if (df.exists()) return d;
        }
        throw new RuntimeException("Can't find an active drive");
    }

    private static void checkDrive(int depth, char drive, boolean exists)
        throws Exception
    {
        String d = drive + ":";
        File df = new File(d);
        String ans = exists ? df.getAbsolutePath() : d;
        if (!ans.endsWith("\\"))
            ans = ans + "\\";
        checkNames(depth, false, ans + relative, d + relative);
    }

    private static void checkDrivePaths() throws Exception {
        checkDrive(2, findActiveDrive(), true);
        checkDrive(2, findInactiveDrive(), false);
    }


    /* UNC pathnames */

    private static void checkUncPaths() throws Exception {
        String s = ("\\\\" + NONEXISTENT_UNC_HOST
                    + "\\" + NONEXISTENT_UNC_SHARE);
        ensureNon(s);
        checkSlashes(DEPTH, false, s, s);

        s = "\\\\" + EXISTENT_UNC_HOST + "\\" + EXISTENT_UNC_SHARE;
        if (!(new File(s)).exists()) {
            System.err.println("WARNING: " + s +
                               " does not exist, unable to test UNC pathnames");
            return;
        }

        checkSlashes(DEPTH, false, s, s);
    }


    public static void main(String[] args) throws Exception {
        if (File.separatorChar != '\\') {
            /* This test is only valid on win32 systems */
            return;
        }
        if (args.length > 0) debug = true;
        userDir = System.getProperty("user.dir") + '\\';
        baseDir = initTestData(6) + '\\';
        relative = baseDir.substring(userDir.length());
        checkRelativePaths();
        checkDrivePaths();
        checkUncPaths();
    }

    private static String initTestData(int maxDepth) throws IOException {
        File parent = new File(userDir);
        String baseDir = null;
        maxDepth = maxDepth < DEPTH + 2 ? DEPTH + 2 : maxDepth;
        for (int i = 0; i < maxDepth; i ++) {
            File dir1 = new File(parent, gensym());
            dir1.mkdir();
            if (i != 0) {
                File dir2 = new File(parent, gensym());
                dir2.mkdir();
                File f1 = new File(parent, gensym());
                f1.createNewFile();
                File f2 = new File(parent, gensym());
                f2.createNewFile();
            }
            if (i == DEPTH + 1) {
                baseDir = dir1.getAbsolutePath();
            }
            parent = dir1;
        }
        return baseDir;
    }
}
