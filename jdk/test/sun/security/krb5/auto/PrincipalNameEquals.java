/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 7061379
 * @summary [Kerberos] Cross-realm authentication fails, due to nameType problem
 * @modules java.base/sun.net.spi.nameservice
 *          java.base/sun.security.util
 *          java.security.jgss/sun.security.jgss
 *          java.security.jgss/sun.security.krb5
 *          java.security.jgss/sun.security.krb5.internal
 *          java.security.jgss/sun.security.krb5.internal.ccache
 *          java.security.jgss/sun.security.krb5.internal.crypto
 *          java.security.jgss/sun.security.krb5.internal.ktab
 * @compile -XDignore.symbol.file PrincipalNameEquals.java
 * @run main/othervm PrincipalNameEquals
 */

import sun.security.jgss.GSSUtil;
import sun.security.krb5.PrincipalName;

public class PrincipalNameEquals {

    public static void main(String[] args) throws Exception {

        OneKDC kdc = new OneKDC(null);
        kdc.writeJAASConf();
        kdc.setOption(KDC.Option.RESP_NT, PrincipalName.KRB_NT_PRINCIPAL);

        Context c, s;
        c = Context.fromJAAS("client");
        s = Context.fromJAAS("server");

        c.startAsClient(OneKDC.SERVER, GSSUtil.GSS_KRB5_MECH_OID);
        s.startAsServer(GSSUtil.GSS_KRB5_MECH_OID);

        Context.handshake(c, s);

        Context.transmit("i say high --", c, s);
        Context.transmit("   you say low", s, c);

        s.dispose();
        c.dispose();
    }
}
