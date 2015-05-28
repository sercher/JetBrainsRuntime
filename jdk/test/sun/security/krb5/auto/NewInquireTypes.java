/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8043071
 * @summary Expose session key and KRB_CRED through extended GSS-API
 * @modules java.base/sun.net.spi.nameservice
 *          java.base/sun.security.util
 *          java.security.jgss/sun.security.jgss
 *          java.security.jgss/sun.security.krb5
 *          java.security.jgss/sun.security.krb5.internal
 *          java.security.jgss/sun.security.krb5.internal.ccache
 *          java.security.jgss/sun.security.krb5.internal.crypto
 *          java.security.jgss/sun.security.krb5.internal.ktab
 * @compile -XDignore.symbol.file NewInquireTypes.java
 * @run main/othervm NewInquireTypes
 */

import com.sun.security.jgss.ExtendedGSSContext;
import com.sun.security.jgss.InquireType;
import sun.security.jgss.GSSUtil;
import sun.security.krb5.internal.KRBCred;
import sun.security.krb5.internal.crypto.KeyUsage;

import javax.security.auth.kerberos.KerberosCredMessage;
import javax.security.auth.kerberos.EncryptionKey;

public class NewInquireTypes {

    public static void main(String[] args) throws Exception {

        new OneKDC(null).writeJAASConf();

        Context c, s;
        c = Context.fromJAAS("client");
        s = Context.fromJAAS("server");

        c.startAsClient(OneKDC.SERVER, GSSUtil.GSS_KRB5_MECH_OID);
        s.startAsServer(GSSUtil.GSS_KRB5_MECH_OID);

        Context.handshake(c, s);

        ExtendedGSSContext ctxt = (ExtendedGSSContext)c.x();
        EncryptionKey key = (EncryptionKey)
                ctxt.inquireSecContext(InquireType.KRB5_GET_SESSION_KEY_EX);
        KerberosCredMessage cred = (KerberosCredMessage)
                ctxt.inquireSecContext(InquireType.KRB5_GET_KRB_CRED);
        c.status();

        // Confirm the KRB_CRED message is encrypted with the session key.
        new KRBCred(cred.getEncoded()).encPart.decrypt(
                new sun.security.krb5.EncryptionKey(key.getKeyType(), key.getEncoded()),
                KeyUsage.KU_ENC_KRB_CRED_PART);
    }
}
