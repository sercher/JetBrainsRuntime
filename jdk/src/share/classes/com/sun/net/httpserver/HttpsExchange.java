/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
 * questions.
 */

package com.sun.net.httpserver;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.net.*;
import javax.net.ssl.*;
import java.util.*;

/**
 * This class encapsulates a HTTPS request received and a
 * response to be generated in one exchange and defines
 * the extensions to HttpExchange that are specific to the HTTPS protocol.
 * @since 1.6
 */

@jdk.Exported
public abstract class HttpsExchange extends HttpExchange {

    protected HttpsExchange () {
    }

    /**
     * Get the SSLSession for this exchange.
     * @return the SSLSession
     */
    public abstract SSLSession getSSLSession ();
}
