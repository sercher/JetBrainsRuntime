/*
 * Copyright 2009 Sun Microsystems, Inc.  All Rights Reserved.
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

/**
 * @test
 * @bug 4167874
 * @library ../../../../com/sun/net/httpserver
 * @build FileServerHandler
 * @run shell build.sh
 * @run main/othervm CloseTest
 * @summary URL-downloaded jar files can consume all available file descriptors
 */

import java.io.*;
import java.net.*;
import java.lang.reflect.*;
import com.sun.net.httpserver.*;

public class CloseTest {

    static void copyFile (String src, String dst) {
        copyFile (new File(src), new File(dst));
    }

    static void copyDir (String src, String dst) {
        copyDir (new File(src), new File(dst));
    }

    static void copyFile (File src, File dst) {
        try {
            if (!src.isFile()) {
                throw new RuntimeException ("File not found: " + src.toString());
            }
            dst.delete();
            dst.createNewFile();
            FileInputStream i = new FileInputStream (src);
            FileOutputStream o = new FileOutputStream (dst);
            byte[] buf = new byte [1024];
            int count;
            while ((count=i.read(buf)) >= 0) {
                o.write (buf, 0, count);
            }
            i.close();
            o.close();
        } catch (IOException e) {
            throw new RuntimeException (e);
        }
    }

    static void rm_minus_rf (File path) {
        if (!path.exists()) {
            return;
        }
        if (path.isFile()) {
            if (!path.delete()) {
                throw new RuntimeException ("Could not delete " + path);
            }
        } else if (path.isDirectory ()) {
            String[] names = path.list();
            File[] files = path.listFiles();
            for (int i=0; i<files.length; i++) {
                rm_minus_rf (new File(path, names[i]));
            }
            if (!path.delete()) {
                throw new RuntimeException ("Could not delete " + path);
            }
        } else {
            throw new RuntimeException ("Trying to delete something that isn't a file or a directory");
        }
    }

    static void copyDir (File src, File dst) {
        if (!src.isDirectory()) {
            throw new RuntimeException ("Dir not found: " + src.toString());
        }
        if (dst.exists()) {
            throw new RuntimeException ("Dir exists: " + dst.toString());
        }
        dst.mkdir();
        String[] names = src.list();
        File[] files = src.listFiles();
        for (int i=0; i<files.length; i++) {
            String f = names[i];
            if (files[i].isDirectory()) {
                copyDir (files[i], new File (dst, f));
            } else {
                copyFile (new File (src, f), new File (dst, f));
            }
        }
    }

    /* expect is true if you expect to find it, false if you expect not to */
    static Class loadClass (String name, URLClassLoader loader, boolean expect){
        try {
            Class clazz = Class.forName (name, true, loader);
            if (!expect) {
                throw new RuntimeException ("loadClass: "+name+" unexpected");
            }
            return clazz;
        } catch (ClassNotFoundException e) {
            if (expect) {
                throw new RuntimeException ("loadClass: " +name + " not found");
            }
        }
        return null;
    }

//
// needs two jar files test1.jar and test2.jar with following structure
//
// com/foo/TestClass
// com/foo/TestClass1
// com/foo/Resource1
// com/foo/Resource2
//
// and a directory hierarchy with the same structure/contents

    public static void main (String args[]) throws Exception {

        String workdir = System.getProperty("test.classes");
        if (workdir == null) {
            workdir = args[0];
        }
        if (!workdir.endsWith("/")) {
            workdir = workdir+"/";
        }

        startHttpServer (workdir+"serverRoot/");

        String testjar = workdir + "test.jar";
        copyFile (workdir+"test1.jar", testjar);
        test (testjar, 1);

        // repeat test with different implementation
        // of test.jar (whose TestClass.getValue() returns 2

        copyFile (workdir+"test2.jar", testjar);
        test (testjar, 2);

        // repeat test using a directory of files
        String testdir=workdir+"testdir/";
        rm_minus_rf (new File(testdir));
        copyDir (workdir+"test1/", testdir);
        test (testdir, 1);

        testdir=workdir+"testdir/";
        rm_minus_rf (new File(testdir));
        copyDir (workdir+"test2/", testdir);
        test (testdir, 2);
        getHttpServer().stop (3);
    }

    // create a loader on jarfile (or directory), plus a http loader
    // load a class , then look for a resource
    // also load a class from http loader
    // then close the loader
    // check further new classes/resources cannot be loaded
    // check jar (or dir) can be deleted
    // check existing classes can be loaded
    // check boot classes can be loaded

    static void test (String name, int expectedValue) throws Exception {
        URL url = new URL ("file", null, name);
        URL url2 = getServerURL();
        System.out.println ("Doing tests with URL: " + url + " and " + url2);
        URL[] urls = new URL[2];
        urls[0] =  url;
        urls[1] =  url2;
        URLClassLoader loader = new URLClassLoader (urls);
        Class testclass = loadClass ("com.foo.TestClass", loader, true);
        Class class2 = loadClass ("Test", loader, true); // from http
        class2.newInstance();
        Object test = testclass.newInstance();
        Method method = testclass.getDeclaredMethods()[0]; // int getValue();
        int res = (Integer) method.invoke (test);

        if (res != expectedValue) {
            throw new RuntimeException ("wrong value from getValue() ["+res+
                        "/"+expectedValue+"]");
        }

        // should find /resource1
        URL u1 = loader.findResource ("com/foo/Resource1");
        if (u1 == null) {
            throw new RuntimeException ("can't find com/foo/Resource1 in test1.jar");
        }
        loader.close ();

        // should NOT find /resource2 even though it is in jar
        URL u2 = loader.findResource ("com/foo/Resource2");
        if (u2 != null) {
            throw new RuntimeException ("com/foo/Resource2 unexpected in test1.jar");
        }

        // load tests
        loadClass ("com.foo.TestClass1", loader, false);
        loadClass ("com.foo.TestClass", loader, true);
        loadClass ("java.awt.Button", loader, true);

        // now check we can delete the path
        rm_minus_rf (new File(name));
        System.out.println (" ... OK");
    }

    static HttpServer httpServer;

    static HttpServer getHttpServer() {
        return httpServer;
    }

    static URL getServerURL () throws Exception {
        int port = httpServer.getAddress().getPort();
        String s = "http://127.0.0.1:"+port+"/";
        return new URL(s);
    }

    static void startHttpServer (String docroot) throws Exception {
        httpServer = HttpServer.create (new InetSocketAddress(0), 10);
        HttpContext ctx = httpServer.createContext (
                "/", new FileServerHandler(docroot)
        );
        httpServer.start();
    }
}
