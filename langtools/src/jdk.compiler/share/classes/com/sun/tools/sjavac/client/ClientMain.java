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

package com.sun.tools.sjavac.client;

import java.io.OutputStreamWriter;
import java.io.Writer;

import com.sun.tools.sjavac.AutoFlushWriter;
import com.sun.tools.sjavac.Log;
import com.sun.tools.sjavac.Util;
import com.sun.tools.sjavac.comp.SjavacImpl;
import com.sun.tools.sjavac.options.Options;
import com.sun.tools.sjavac.server.Sjavac;

/**
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class ClientMain {

    public static int run(String[] args) {
        return run(args,
                   new AutoFlushWriter(new OutputStreamWriter(System.out)),
                   new AutoFlushWriter(new OutputStreamWriter(System.err)));
    }

    public static int run(String[] args, Writer out, Writer err) {

        Log.initializeLog(out, err);

        Options options;
        try {
            options = Options.parseArgs(args);
        } catch (IllegalArgumentException e) {
            Log.error(e.getMessage());
            return -1;
        }

        Log.debug("==========================================================");
        Log.debug("Launching sjavac client with the following parameters:");
        Log.debug("    " + options.getStateArgsString());
        Log.debug("==========================================================");

        // Prepare sjavac object
        boolean background = Util.extractBooleanOption("background", options.getServerConf(), true);
        Sjavac sjavac;
        // Create an sjavac implementation to be used for compilation
        if (background) {
            try {
                sjavac = new SjavacClient(options);
            } catch (PortFileInaccessibleException e) {
                Log.error("Port file inaccessible.");
                return -1;
            }
        } else {
            sjavac = new SjavacImpl();
        }

        int rc = sjavac.compile(args, out, err);

        if (!background)
            sjavac.shutdown();

        return rc;
    }
}
