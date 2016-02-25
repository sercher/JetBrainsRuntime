/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import java.io.*;
import java.net.*;
import java.security.*;
import javax.net.ssl.*;
import com.sun.net.httpserver.*;

/**
 * Implements a basic static content HTTP server
 * which understands text/html, text/plain content types
 *
 * Must be given an abs pathname to the document root.
 * Directory listings together with text + html files
 * can be served.
 *
 * File Server created on files sub-path
 *
 * Echo server created on echo sub-path
 */
public class FileServerHandler implements HttpHandler {

        public static void main (String[] args) throws Exception {
            if (args.length != 3) {
                System.out.println ("usage: java FileServerHandler rootDir port logfilename");
                System.exit(1);
            }
            Logger logger = Logger.getLogger("com.sun.net.httpserver");
            ConsoleHandler ch = new ConsoleHandler();
            logger.setLevel(Level.ALL);
            ch.setLevel(Level.ALL);
            logger.addHandler(ch);

            String rootDir = args[0];
            int port = Integer.parseInt (args[1]);
            String logfile = args[2];
            HttpServer server = HttpServer.create (new InetSocketAddress (port), 0);
            HttpHandler h = new FileServerHandler (rootDir);
            HttpHandler h1 = new EchoHandler ();

            HttpContext c = server.createContext ("/files", h);
            c.getFilters().add (new LogFilter (new File (logfile)));
            HttpContext c1 = server.createContext ("/echo", h1);
            c.getFilters().add (new LogFilter (new File (logfile)));
            c1.getFilters().add (new LogFilter (new File (logfile)));
            server.setExecutor (Executors.newCachedThreadPool());
            server.start ();
        }

        String docroot;

        FileServerHandler (String docroot) {
            this.docroot = docroot;
        }

        int invocation = 1;
        public void handle (HttpExchange t)
            throws IOException
        {
            InputStream is = t.getRequestBody();
            Headers map = t.getRequestHeaders();
            Headers rmap = t.getResponseHeaders();
            URI uri = t.getRequestURI();
            String path = uri.getPath();

            int x = 0;
            while (is.read () != -1) x++;
            is.close();
            File f = new File (docroot, path);
            if (!f.exists()) {
                notfound (t, path);
                return;
            }
            String fixedrequest = map.getFirst ("XFixed");

            String method = t.getRequestMethod();
            if (method.equals ("HEAD")) {
                rmap.set ("Content-Length", Long.toString (f.length()));
                t.sendResponseHeaders (200, -1);
                t.close();
            } else if (!method.equals("GET")) {
                t.sendResponseHeaders (405, -1);
                t.close();
                return;
            }

            if (path.endsWith (".html") || path.endsWith (".htm")) {
                rmap.set ("Content-Type", "text/html");
            } else {
                rmap.set ("Content-Type", "text/plain");
            }
            if (f.isDirectory()) {
                if (!path.endsWith ("/")) {
                    moved (t);
                    return;
                }
                rmap.set ("Content-Type", "text/html");
                t.sendResponseHeaders (200, 0);
                String[] list = f.list();
                OutputStream os = t.getResponseBody();
                PrintStream p = new PrintStream (os);
                p.println ("<h2>Directory listing for: " + path+ "</h2>");
                p.println ("<ul>");
                for (int i=0; i<list.length; i++) {
                    p.println ("<li><a href=\""+list[i]+"\">"+list[i]+"</a></li>");
                }
                p.println ("</ul><p><hr>");
                p.flush();
                p.close();
            } else {
                int clen;
                if (fixedrequest != null) {
                    clen = (int) f.length();
                } else {
                    clen = 0;
                }
                t.sendResponseHeaders (200, clen);
                OutputStream os = t.getResponseBody();
                FileInputStream fis = new FileInputStream (f);
                int count = 0;
                try {
                byte[] buf = new byte [16 * 1024];
                int len;
                while ((len=fis.read (buf)) != -1) {
                    os.write (buf, 0, len);
                    count += len;
                }
                } catch (IOException e) {
                        e.printStackTrace();
                }
                fis.close();
                os.close();
            }
        }

        void moved (HttpExchange t) throws IOException {
            Headers req = t.getRequestHeaders();
            Headers map = t.getResponseHeaders();
            URI uri = t.getRequestURI();
            String host = req.getFirst ("Host");
            String location = "http://"+host+uri.getPath() + "/";
            map.set ("Content-Type", "text/html");
            map.set ("Location", location);
            t.sendResponseHeaders (301, -1);
            t.close();
        }

        void notfound (HttpExchange t, String p) throws IOException {
            t.getResponseHeaders().set ("Content-Type", "text/html");
            t.sendResponseHeaders (404, 0);
            OutputStream os = t.getResponseBody();
            String s = "<h2>File not found</h2>";
            s = s + p + "<p>";
            os.write (s.getBytes());
            os.close();
            t.close();
        }
    }

class EchoHandler implements HttpHandler {

    byte[] read(InputStream is) throws IOException {
        byte[] buf = new byte[1024];
        byte[] result = new byte[0];

        while (true) {
            int n = is.read(buf);
            if (n > 0) {
                byte[] b1 = new byte[result.length + n];
                System.arraycopy(result, 0, b1, 0, result.length);
                System.arraycopy(buf, 0, b1, result.length, n);
                result = b1;
            } else if (n == -1) {
                return result;
            }
        }
    }

    public void handle (HttpExchange t)
        throws IOException
    {
        InputStream is = t.getRequestBody();
        Headers map = t.getRequestHeaders();
        String fixedrequest = map.getFirst ("XFixed");

        // return the number of bytes received (no echo)
        String summary = map.getFirst ("XSummary");
        if (fixedrequest != null && summary == null)  {
            byte[] in = read(is);
            t.sendResponseHeaders(200, in.length);
            OutputStream os = t.getResponseBody();
            os.write(in);
            os.close();
            is.close();
        } else {
            OutputStream os = t.getResponseBody();
            byte[] buf = new byte[64 * 1024];
            t.sendResponseHeaders(200, 0);
            int n, count=0;;

            while ((n = is.read(buf)) != -1) {
                if (summary == null) {
                    os.write(buf, 0, n);
                }
                count += n;
            }
            if (summary != null) {
                String s = Integer.toString(count);
                os.write(s.getBytes());
            }
            os.close();
            is.close();
        }
    }
}

