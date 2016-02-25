/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 */
package java.net.http;

import java.util.Locale;
import sun.util.logging.PlatformLogger;

/**
 * -Djava.net.HttpClient.log=errors,requests,headers,frames[:type:type2:..],content
 *
 * Any of errors, requests, headers or content are optional.
 *
 * Other handlers may be added. All logging is at level INFO
 *
 * Logger name is "java.net.http.HttpClient"
 */
class Log {

    final static String logProp = "java.net.http.HttpClient.log";

    public static final int OFF = 0;
    public static final int ERRORS = 0x1;
    public static final int REQUESTS = 0x2;
    public static final int HEADERS = 0x4;
    public static final int CONTENT = 0x8;
    public static final int FRAMES = 0x10;
    public static final int SSL = 0x20;
    static int logging;

    // Frame types: "control", "data", "window", "all"
    public static final int CONTROL = 1; // all except DATA and WINDOW_UPDATES
    public static final int DATA = 2;
    public static final int WINDOW_UPDATES = 4;
    public static final int ALL = CONTROL| DATA | WINDOW_UPDATES;
    static int frametypes;

    static sun.util.logging.PlatformLogger logger;

    static {
        String s = Utils.getNetProperty(logProp);
        if (s == null) {
            logging = OFF;
        } else {
            String[] vals = s.split(",");
            for (String val : vals) {
                switch (val.toLowerCase(Locale.US)) {
                    case "errors":
                        logging |= ERRORS;
                        break;
                    case "requests":
                        logging |= REQUESTS;
                        break;
                    case "headers":
                        logging |= HEADERS;
                        break;
                    case "content":
                        logging |= CONTENT;
                        break;
                    case "ssl":
                        logging |= SSL;
                        break;
                    case "all":
                        logging |= CONTENT|HEADERS|REQUESTS|FRAMES|ERRORS;
                        break;
                }
                if (val.startsWith("frames")) {
                    logging |= FRAMES;
                    String[] types = val.split(":");
                    if (types.length == 1) {
                        frametypes = CONTROL | DATA | WINDOW_UPDATES;
                    } else {
                        for (String type : types) {
                            switch (type.toLowerCase()) {
                                case "control":
                                    frametypes |= CONTROL;
                                    break;
                                case "data":
                                    frametypes |= DATA;
                                    break;
                                case "window":
                                    frametypes |= WINDOW_UPDATES;
                                    break;
                                case "all":
                                    frametypes = ALL;
                                    break;
                            }
                        }
                    }
                }
            }
        }
        if (logging != OFF) {
            logger = PlatformLogger.getLogger("java.net.http.HttpClient");
        }
    }

    static boolean errors() {
        return (logging & ERRORS) != 0;
    }

    static boolean requests() {
        return (logging & REQUESTS) != 0;
    }

    static boolean headers() {
        return (logging & HEADERS) != 0;
    }

    static boolean ssl() {
        return (logging & SSL) != 0;
    }

    static boolean frames() {
        return (logging & FRAMES) != 0;
    }

    static void logError(String s) {
        if (errors())
            logger.info("ERROR: " + s);
    }

    static void logError(Throwable t) {
        if (errors()) {
            String s = Utils.stackTrace(t);
            logger.info("ERROR: " + s);
        }
    }

    static void logSSL(String s) {
        if (ssl())
            logger.info("SSL: " + s);
    }

    static void logRequest(String s) {
        if (requests())
            logger.info("REQUEST: " + s);
    }

    static void logResponse(String s) {
        if (requests())
            logger.info("RESPONSE: " + s);
    }

    static void logHeaders(String s) {
        if (headers())
            logger.info("HEADERS: " + s);
    }
// END HTTP2
}
