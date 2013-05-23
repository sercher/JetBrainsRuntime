/*
 * Copyright (c) 2005, 2012, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.jgss.wrapper;

import java.util.HashMap;
import java.security.Provider;
import java.security.AccessController;
import java.security.PrivilegedAction;
import org.ietf.jgss.Oid;
import sun.security.action.PutAllAction;

/**
 * Defines the Sun NativeGSS provider for plugging in the
 * native GSS mechanisms to Java GSS.
 *
 * List of supported mechanisms depends on the local
 * machine configuration.
 *
 * @author Yu-Ching Valerie Peng
 */

public final class SunNativeProvider extends Provider {

    private static final long serialVersionUID = -238911724858694204L;

    private static final String NAME = "SunNativeGSS";
    private static final String INFO = "Sun Native GSS provider";
    private static final String MF_CLASS =
        "sun.security.jgss.wrapper.NativeGSSFactory";
    private static final String LIB_PROP = "sun.security.jgss.lib";
    private static final String DEBUG_PROP = "sun.security.nativegss.debug";
    private static HashMap<String, String> MECH_MAP;
    static final Provider INSTANCE = new SunNativeProvider();
    static boolean DEBUG;
    static void debug(String message) {
        if (DEBUG) {
            if (message == null) {
                throw new NullPointerException();
            }
            System.out.println(NAME + ": " + message);
        }
    }

    static {
        MECH_MAP =
            AccessController.doPrivileged(
                new PrivilegedAction<HashMap<String, String>>() {
                    public HashMap<String, String> run() {
                        DEBUG = Boolean.parseBoolean
                            (System.getProperty(DEBUG_PROP));
                        try {
                            System.loadLibrary("j2gss");
                        } catch (Error err) {
                            debug("No j2gss library found!");
                            if (DEBUG) err.printStackTrace();
                            return null;
                        }
                        String gssLibs[] = new String[0];
                        String defaultLib = System.getProperty(LIB_PROP);
                        if (defaultLib == null || defaultLib.trim().equals("")) {
                            String osname = System.getProperty("os.name");
                            if (osname.startsWith("SunOS")) {
                                gssLibs = new String[]{ "libgss.so" };
                            } else if (osname.startsWith("Linux")) {
                                gssLibs = new String[]{
                                    "libgssapi.so",
                                    "libgssapi_krb5.so",
                                    "libgssapi_krb5.so.2",
                                };
                            } else if (osname.contains("OS X")) {
                                gssLibs = new String[]{
                                    "libgssapi_krb5.dylib",
                                    "/usr/lib/sasl2/libgssapiv2.2.so",
                               };
                            }
                        } else {
                            gssLibs = new String[]{ defaultLib };
                        }
                        for (String libName: gssLibs) {
                            if (GSSLibStub.init(libName)) {
                                debug("Loaded GSS library: " + libName);
                                Oid[] mechs = GSSLibStub.indicateMechs();
                                HashMap<String, String> map =
                                            new HashMap<String, String>();
                                for (int i = 0; i < mechs.length; i++) {
                                    debug("Native MF for " + mechs[i]);
                                    map.put("GssApiMechanism." + mechs[i],
                                            MF_CLASS);
                                }
                                return map;
                            }
                        }
                        return null;
                    }
                });
    }

    public SunNativeProvider() {
        /* We are the Sun NativeGSS provider */
        super(NAME, 1.0, INFO);

        if (MECH_MAP != null) {
            AccessController.doPrivileged(new PutAllAction(this, MECH_MAP));
        }
    }
}
