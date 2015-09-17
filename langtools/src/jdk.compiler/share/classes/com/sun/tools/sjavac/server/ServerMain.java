/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.sjavac.server;

import java.io.IOException;
import java.io.OutputStreamWriter;

import com.sun.tools.sjavac.Log;

/**
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class ServerMain {
    public static int run(String[] args) {

        Log.initializeLog(new OutputStreamWriter(System.out),
                          new OutputStreamWriter(System.err));

        // Any options other than --startserver?
        if (args.length > 1) {
            System.err.println("When spawning a background server, only a single --startserver argument is allowed.");
            return 1;
        }

        int exitCode;
        try {
            SjavacServer server = new SjavacServer(args[0], System.err);
            exitCode = server.startServer();
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
            exitCode = -1;
        }

        return exitCode;
    }
}
