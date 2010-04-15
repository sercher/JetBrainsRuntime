/*
 * Copyright 2003-2004 Sun Microsystems, Inc.  All Rights Reserved.
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

/* @test
 * @summary getResponseCode() doesn't return correct value when using cached response
 * @bug 4921268
 * @author Yingxian Wang
 */

import java.net.*;
import java.util.*;
import java.io.*;


/**
 * Request should get serviced by the cache handler. Response get
 * saved through the cache handler.
 */
public class getResponseCode {
    static URL url;
    static String FNPrefix;

    getResponseCode() throws Exception {
        url = new URL("http://localhost/file1.cache");
        HttpURLConnection http = (HttpURLConnection)url.openConnection();
        int respCode = http.getResponseCode();
        http.disconnect();

        if (respCode != 200) {
            throw new RuntimeException("Response code should return 200, but it is returning "+respCode);
        }
    }
    public static void main(String args[]) throws Exception {
        try {
            ResponseCache.setDefault(new MyResponseCache());
            FNPrefix = System.getProperty("test.src", ".")+"/";
            new getResponseCode();
        } finally{
            ResponseCache.setDefault(null);
        }
    }

    static class MyResponseCache extends ResponseCache {
        public CacheResponse
        get(URI uri, String rqstMethod, Map<String,List<String>> requestHeaders)
            throws IOException {
            return new MyResponse(FNPrefix+"file1.cache");
        }
        public CacheRequest put(URI uri, URLConnection uconn)  throws IOException {;
            return null;
        }
    }

    static class MyResponse extends CacheResponse {
        FileInputStream fis;
        Map<String,List<String>> headers;
        public MyResponse(String filename) {
            try {
                fis = new FileInputStream(new File(filename));
                headers = (Map<String,List<String>>)new ObjectInputStream(fis).readObject();
            } catch (Exception ex) {
                throw new RuntimeException(ex.getMessage());
            }
        }
        public InputStream getBody() throws IOException {
            return fis;
        }

        public Map<String,List<String>> getHeaders() throws IOException {
            return headers;
        }
    }
}
