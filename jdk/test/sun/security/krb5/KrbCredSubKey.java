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

/**
 * @test
 * @bug 7030180
 * @run main/othervm KrbCredSubKey
 * @summary AES 128/256 decrypt exception
 */

import java.io.FileOutputStream;
import java.security.PrivilegedExceptionAction;
import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSManager;
import sun.security.jgss.GSSUtil;
import sun.security.krb5.Config;
import sun.security.krb5.EncryptedData;

public class KrbCredSubKey {

    public static void main(String[] args) throws Exception {

        // We don't care about clock difference
        new FileOutputStream("krb5.conf").write(
                "[libdefaults]\nclockskew=999999999".getBytes());
        System.setProperty("java.security.krb5.conf", "krb5.conf");
        Config.refresh();

        Subject subj = new Subject();
        KerberosPrincipal kp = new KerberosPrincipal(princ);
        KerberosKey kk = new KerberosKey(
                kp, key, EncryptedData.ETYPE_AES128_CTS_HMAC_SHA1_96, 0);
        subj.getPrincipals().add(kp);
        subj.getPrivateCredentials().add(kk);

        Subject.doAs(subj, new PrivilegedExceptionAction() {
            public Object run() throws Exception {
                GSSManager man = GSSManager.getInstance();
                GSSContext ctxt = man.createContext(man.createCredential(
                        null, GSSCredential.INDEFINITE_LIFETIME,
                        GSSUtil.GSS_KRB5_MECH_OID, GSSCredential.ACCEPT_ONLY));
                return ctxt.acceptSecContext(token, 0, token.length);
            }
        });
    }

    // All following data generated by myself on a test machine

    private static String princ = "server/host.rabbit.hole@RABBIT.HOLE";

    // A aes-128 key for princ
    private static byte[] key = {
            (byte)0x83, (byte)0xA1, (byte)0xD6, (byte)0xE2,
            (byte)0xC7, (byte)0x76, (byte)0xD5, (byte)0x24,
            (byte)0x63, (byte)0x9F, (byte)0xF9, (byte)0xFF,
            (byte)0x76, (byte)0x6D, (byte)0x26, (byte)0x30,
    };

    // A JGSS token generated by the first call of an initiator's
    // initSecContext, targetting princ, using the authenticator
    // subkey to encrypt the KRB_CRED inside AP_REQ
    private static byte[] token = {
            (byte)0x60, (byte)0x82, (byte)0x04, (byte)0x1C,
            (byte)0x06, (byte)0x09, (byte)0x2A, (byte)0x86,
            (byte)0x48, (byte)0x86, (byte)0xF7, (byte)0x12,
            (byte)0x01, (byte)0x02, (byte)0x02, (byte)0x01,
            (byte)0x00, (byte)0x6E, (byte)0x82, (byte)0x04,
            (byte)0x0B, (byte)0x30, (byte)0x82, (byte)0x04,
            (byte)0x07, (byte)0xA0, (byte)0x03, (byte)0x02,
            (byte)0x01, (byte)0x05, (byte)0xA1, (byte)0x03,
            (byte)0x02, (byte)0x01, (byte)0x0E, (byte)0xA2,
            (byte)0x07, (byte)0x03, (byte)0x05, (byte)0x00,
            (byte)0x20, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0xA3, (byte)0x82, (byte)0x01, (byte)0x04,
            (byte)0x61, (byte)0x82, (byte)0x01, (byte)0x00,
            (byte)0x30, (byte)0x81, (byte)0xFD, (byte)0xA0,
            (byte)0x03, (byte)0x02, (byte)0x01, (byte)0x05,
            (byte)0xA1, (byte)0x0D, (byte)0x1B, (byte)0x0B,
            (byte)0x52, (byte)0x41, (byte)0x42, (byte)0x42,
            (byte)0x49, (byte)0x54, (byte)0x2E, (byte)0x48,
            (byte)0x4F, (byte)0x4C, (byte)0x45, (byte)0xA2,
            (byte)0x25, (byte)0x30, (byte)0x23, (byte)0xA0,
            (byte)0x03, (byte)0x02, (byte)0x01, (byte)0x00,
            (byte)0xA1, (byte)0x1C, (byte)0x30, (byte)0x1A,
            (byte)0x1B, (byte)0x06, (byte)0x73, (byte)0x65,
            (byte)0x72, (byte)0x76, (byte)0x65, (byte)0x72,
            (byte)0x1B, (byte)0x10, (byte)0x68, (byte)0x6F,
            (byte)0x73, (byte)0x74, (byte)0x2E, (byte)0x72,
            (byte)0x61, (byte)0x62, (byte)0x62, (byte)0x69,
            (byte)0x74, (byte)0x2E, (byte)0x68, (byte)0x6F,
            (byte)0x6C, (byte)0x65, (byte)0xA3, (byte)0x81,
            (byte)0xBF, (byte)0x30, (byte)0x81, (byte)0xBC,
            (byte)0xA0, (byte)0x03, (byte)0x02, (byte)0x01,
            (byte)0x11, (byte)0xA2, (byte)0x81, (byte)0xB4,
            (byte)0x04, (byte)0x81, (byte)0xB1, (byte)0xA7,
            (byte)0xE8, (byte)0x58, (byte)0xBA, (byte)0x98,
            (byte)0x69, (byte)0x45, (byte)0xB3, (byte)0x68,
            (byte)0xBF, (byte)0xFD, (byte)0x25, (byte)0x74,
            (byte)0xC4, (byte)0x2E, (byte)0x09, (byte)0x7B,
            (byte)0x3C, (byte)0x7F, (byte)0xA5, (byte)0x6C,
            (byte)0xC3, (byte)0x86, (byte)0xC9, (byte)0xEE,
            (byte)0x58, (byte)0xD3, (byte)0x7C, (byte)0xD6,
            (byte)0x19, (byte)0xA1, (byte)0x3B, (byte)0xF7,
            (byte)0x17, (byte)0xD6, (byte)0x18, (byte)0xA9,
            (byte)0x58, (byte)0x43, (byte)0x55, (byte)0xD6,
            (byte)0xBA, (byte)0x85, (byte)0xF7, (byte)0x6B,
            (byte)0x20, (byte)0x01, (byte)0xEF, (byte)0xB4,
            (byte)0x74, (byte)0x0B, (byte)0x31, (byte)0x07,
            (byte)0x55, (byte)0xD8, (byte)0x8C, (byte)0x85,
            (byte)0x25, (byte)0x12, (byte)0x66, (byte)0x85,
            (byte)0xA8, (byte)0x5A, (byte)0x84, (byte)0xB2,
            (byte)0x6C, (byte)0xDE, (byte)0xEE, (byte)0xF9,
            (byte)0x15, (byte)0xF2, (byte)0xBC, (byte)0xB0,
            (byte)0x43, (byte)0xA5, (byte)0x21, (byte)0x31,
            (byte)0xFA, (byte)0x2F, (byte)0x2C, (byte)0x37,
            (byte)0x39, (byte)0xD8, (byte)0xAA, (byte)0xE0,
            (byte)0x78, (byte)0x08, (byte)0x18, (byte)0xFB,
            (byte)0x03, (byte)0x43, (byte)0x22, (byte)0xE6,
            (byte)0x2C, (byte)0xF2, (byte)0x98, (byte)0xDC,
            (byte)0x2A, (byte)0xDE, (byte)0x8C, (byte)0x95,
            (byte)0x0B, (byte)0xB6, (byte)0xE6, (byte)0x0F,
            (byte)0xB5, (byte)0x4E, (byte)0xAD, (byte)0xAC,
            (byte)0xD1, (byte)0x4C, (byte)0xE8, (byte)0x22,
            (byte)0x93, (byte)0x38, (byte)0xA2, (byte)0x44,
            (byte)0x0E, (byte)0x83, (byte)0x9E, (byte)0x4D,
            (byte)0xC0, (byte)0x1A, (byte)0x02, (byte)0xB2,
            (byte)0xB8, (byte)0xCE, (byte)0xDF, (byte)0xB5,
            (byte)0xFB, (byte)0xF2, (byte)0x75, (byte)0x5E,
            (byte)0x74, (byte)0xC1, (byte)0x90, (byte)0x82,
            (byte)0x60, (byte)0x00, (byte)0xA5, (byte)0xC3,
            (byte)0xBF, (byte)0x66, (byte)0x97, (byte)0x0E,
            (byte)0xF3, (byte)0x9F, (byte)0xB3, (byte)0xD9,
            (byte)0x51, (byte)0x51, (byte)0x38, (byte)0xBC,
            (byte)0xD9, (byte)0xC1, (byte)0xD0, (byte)0x1E,
            (byte)0x90, (byte)0x9B, (byte)0x43, (byte)0xEE,
            (byte)0xD9, (byte)0xD6, (byte)0x3E, (byte)0x31,
            (byte)0xEA, (byte)0x8E, (byte)0xB1, (byte)0xDC,
            (byte)0xDE, (byte)0xFD, (byte)0xA4, (byte)0x77,
            (byte)0x6C, (byte)0x4A, (byte)0x81, (byte)0x1F,
            (byte)0xA4, (byte)0x82, (byte)0x02, (byte)0xE8,
            (byte)0x30, (byte)0x82, (byte)0x02, (byte)0xE4,
            (byte)0xA0, (byte)0x03, (byte)0x02, (byte)0x01,
            (byte)0x11, (byte)0xA2, (byte)0x82, (byte)0x02,
            (byte)0xDB, (byte)0x04, (byte)0x82, (byte)0x02,
            (byte)0xD7, (byte)0x81, (byte)0x78, (byte)0x25,
            (byte)0x75, (byte)0x92, (byte)0x7A, (byte)0xEC,
            (byte)0xBE, (byte)0x31, (byte)0xF1, (byte)0x50,
            (byte)0xE7, (byte)0xC1, (byte)0x32, (byte)0xA5,
            (byte)0xCB, (byte)0x34, (byte)0x46, (byte)0x95,
            (byte)0x2B, (byte)0x84, (byte)0xB7, (byte)0x06,
            (byte)0x0E, (byte)0x15, (byte)0x02, (byte)0x74,
            (byte)0xCA, (byte)0x18, (byte)0x5D, (byte)0xE8,
            (byte)0x0E, (byte)0x1B, (byte)0xB7, (byte)0x77,
            (byte)0x5A, (byte)0x6C, (byte)0xFB, (byte)0x94,
            (byte)0x82, (byte)0x2B, (byte)0xE6, (byte)0x14,
            (byte)0x0C, (byte)0xDA, (byte)0x22, (byte)0xA2,
            (byte)0x42, (byte)0xD7, (byte)0xB0, (byte)0xFC,
            (byte)0xCA, (byte)0x4A, (byte)0xEA, (byte)0xB8,
            (byte)0x92, (byte)0xB5, (byte)0x8C, (byte)0x71,
            (byte)0xED, (byte)0x2B, (byte)0x46, (byte)0xC5,
            (byte)0xE5, (byte)0x47, (byte)0x76, (byte)0x29,
            (byte)0x27, (byte)0x0F, (byte)0xFF, (byte)0x03,
            (byte)0x72, (byte)0x13, (byte)0xAA, (byte)0xDB,
            (byte)0x4E, (byte)0xFF, (byte)0x48, (byte)0x36,
            (byte)0xAB, (byte)0x73, (byte)0xD7, (byte)0xDA,
            (byte)0xF1, (byte)0x80, (byte)0x1B, (byte)0x5B,
            (byte)0x9A, (byte)0x88, (byte)0x07, (byte)0x47,
            (byte)0x43, (byte)0x27, (byte)0xD5, (byte)0x00,
            (byte)0x04, (byte)0xEE, (byte)0xAF, (byte)0x53,
            (byte)0x5C, (byte)0xCC, (byte)0x2C, (byte)0xC7,
            (byte)0x2F, (byte)0x94, (byte)0x12, (byte)0x86,
            (byte)0xEF, (byte)0xAC, (byte)0xB1, (byte)0x6C,
            (byte)0xB0, (byte)0xB5, (byte)0x3D, (byte)0x92,
            (byte)0xBD, (byte)0xBE, (byte)0x7B, (byte)0x1A,
            (byte)0x39, (byte)0x4A, (byte)0x1E, (byte)0x91,
            (byte)0xA4, (byte)0xDF, (byte)0x82, (byte)0x12,
            (byte)0x2E, (byte)0x67, (byte)0x17, (byte)0x92,
            (byte)0xB3, (byte)0x93, (byte)0x38, (byte)0x32,
            (byte)0x94, (byte)0xF5, (byte)0xF7, (byte)0x09,
            (byte)0x07, (byte)0x5E, (byte)0x21, (byte)0x12,
            (byte)0x70, (byte)0x37, (byte)0xAF, (byte)0x5A,
            (byte)0x2D, (byte)0xAC, (byte)0xFF, (byte)0x22,
            (byte)0x46, (byte)0xA0, (byte)0x12, (byte)0x74,
            (byte)0x1C, (byte)0xA1, (byte)0x68, (byte)0xC3,
            (byte)0x64, (byte)0xDB, (byte)0xC3, (byte)0x9F,
            (byte)0xAB, (byte)0x0E, (byte)0x19, (byte)0xFE,
            (byte)0xD9, (byte)0xA4, (byte)0xAA, (byte)0x7B,
            (byte)0x73, (byte)0xAD, (byte)0xC8, (byte)0xA8,
            (byte)0xD5, (byte)0x29, (byte)0xAD, (byte)0x1F,
            (byte)0xEF, (byte)0x54, (byte)0xAE, (byte)0x72,
            (byte)0x02, (byte)0xD9, (byte)0x06, (byte)0x0D,
            (byte)0x1A, (byte)0x94, (byte)0x7B, (byte)0xBC,
            (byte)0x32, (byte)0x9A, (byte)0xBC, (byte)0x4B,
            (byte)0x33, (byte)0xC2, (byte)0x02, (byte)0xA3,
            (byte)0xF4, (byte)0xB1, (byte)0xED, (byte)0x76,
            (byte)0x0D, (byte)0x59, (byte)0xCD, (byte)0x56,
            (byte)0xCB, (byte)0xDC, (byte)0xCE, (byte)0xED,
            (byte)0xFF, (byte)0x25, (byte)0x84, (byte)0x5E,
            (byte)0x41, (byte)0xF9, (byte)0x42, (byte)0xBE,
            (byte)0x73, (byte)0xAC, (byte)0xA2, (byte)0x20,
            (byte)0x97, (byte)0xB7, (byte)0x88, (byte)0x77,
            (byte)0x65, (byte)0x43, (byte)0x9F, (byte)0xEE,
            (byte)0xF4, (byte)0x3A, (byte)0x7E, (byte)0x9B,
            (byte)0x5B, (byte)0x54, (byte)0xD3, (byte)0x0D,
            (byte)0x50, (byte)0x6D, (byte)0xF6, (byte)0x14,
            (byte)0xB7, (byte)0x5A, (byte)0x34, (byte)0x0F,
            (byte)0x1F, (byte)0xC7, (byte)0x39, (byte)0x99,
            (byte)0x9B, (byte)0x96, (byte)0xE3, (byte)0xAD,
            (byte)0x86, (byte)0xE3, (byte)0x6A, (byte)0x71,
            (byte)0x63, (byte)0x04, (byte)0xAD, (byte)0x9C,
            (byte)0x17, (byte)0x68, (byte)0x44, (byte)0xFE,
            (byte)0x21, (byte)0x62, (byte)0xD5, (byte)0x99,
            (byte)0x4A, (byte)0xDF, (byte)0x48, (byte)0xDE,
            (byte)0x9A, (byte)0xD4, (byte)0xBB, (byte)0xA1,
            (byte)0x9B, (byte)0xE7, (byte)0x2A, (byte)0x08,
            (byte)0x80, (byte)0x3A, (byte)0x08, (byte)0xA4,
            (byte)0xBA, (byte)0xBE, (byte)0x1E, (byte)0x81,
            (byte)0x63, (byte)0x20, (byte)0xAC, (byte)0x9C,
            (byte)0x42, (byte)0x2F, (byte)0xCA, (byte)0x06,
            (byte)0x95, (byte)0x92, (byte)0x97, (byte)0x09,
            (byte)0x3C, (byte)0x0C, (byte)0x5A, (byte)0x99,
            (byte)0xFB, (byte)0xAB, (byte)0xEB, (byte)0xDE,
            (byte)0xC4, (byte)0x09, (byte)0xD3, (byte)0xA3,
            (byte)0xF0, (byte)0x65, (byte)0xDC, (byte)0x5F,
            (byte)0xAA, (byte)0xBB, (byte)0x28, (byte)0xC0,
            (byte)0x3E, (byte)0xBF, (byte)0x77, (byte)0xAE,
            (byte)0xCC, (byte)0x3A, (byte)0xD3, (byte)0x31,
            (byte)0x0D, (byte)0x9B, (byte)0x96, (byte)0xEF,
            (byte)0x2C, (byte)0xED, (byte)0x60, (byte)0x63,
            (byte)0xC5, (byte)0x8F, (byte)0xCA, (byte)0xB0,
            (byte)0xA2, (byte)0x0B, (byte)0x49, (byte)0x5A,
            (byte)0xB2, (byte)0x8F, (byte)0xEF, (byte)0xE4,
            (byte)0x19, (byte)0xC0, (byte)0xC6, (byte)0x2D,
            (byte)0xD3, (byte)0x4F, (byte)0xB2, (byte)0xED,
            (byte)0xA3, (byte)0xA4, (byte)0x6F, (byte)0xAE,
            (byte)0xD4, (byte)0xE9, (byte)0xA2, (byte)0x5A,
            (byte)0xFB, (byte)0xB0, (byte)0x14, (byte)0xBD,
            (byte)0x06, (byte)0x12, (byte)0xD7, (byte)0x91,
            (byte)0x15, (byte)0x46, (byte)0x78, (byte)0xE4,
            (byte)0xD1, (byte)0x73, (byte)0xCA, (byte)0xA5,
            (byte)0xA5, (byte)0x64, (byte)0xC8, (byte)0x6F,
            (byte)0xD1, (byte)0xBD, (byte)0xEA, (byte)0x74,
            (byte)0xE4, (byte)0xCA, (byte)0x40, (byte)0x16,
            (byte)0x9E, (byte)0x46, (byte)0x7C, (byte)0x25,
            (byte)0x6C, (byte)0x32, (byte)0xB4, (byte)0x14,
            (byte)0xF9, (byte)0x26, (byte)0x8A, (byte)0x3A,
            (byte)0xDD, (byte)0x51, (byte)0x26, (byte)0x79,
            (byte)0x43, (byte)0x27, (byte)0x2E, (byte)0xED,
            (byte)0xC7, (byte)0x82, (byte)0x7C, (byte)0xCE,
            (byte)0x43, (byte)0x03, (byte)0x60, (byte)0x2A,
            (byte)0x9C, (byte)0xB2, (byte)0x71, (byte)0x41,
            (byte)0xAB, (byte)0x3D, (byte)0xA6, (byte)0xB5,
            (byte)0x51, (byte)0xBC, (byte)0x80, (byte)0x1F,
            (byte)0x96, (byte)0x73, (byte)0x23, (byte)0x11,
            (byte)0xED, (byte)0xC0, (byte)0x1D, (byte)0x0B,
            (byte)0xA0, (byte)0x13, (byte)0xB3, (byte)0x2F,
            (byte)0x16, (byte)0x59, (byte)0x64, (byte)0x45,
            (byte)0xE8, (byte)0x68, (byte)0xFB, (byte)0xF9,
            (byte)0x6F, (byte)0xB0, (byte)0x2B, (byte)0xFB,
            (byte)0x39, (byte)0xBB, (byte)0x53, (byte)0x8F,
            (byte)0xD2, (byte)0xAF, (byte)0x38, (byte)0x5E,
            (byte)0xEF, (byte)0x5B, (byte)0xE2, (byte)0x98,
            (byte)0xE8, (byte)0x46, (byte)0x3C, (byte)0x03,
            (byte)0x71, (byte)0x46, (byte)0x8D, (byte)0x41,
            (byte)0x92, (byte)0x32, (byte)0x85, (byte)0x8D,
            (byte)0xBA, (byte)0x33, (byte)0x05, (byte)0xB1,
            (byte)0xE4, (byte)0x56, (byte)0x3E, (byte)0xF5,
            (byte)0x20, (byte)0x35, (byte)0xA6, (byte)0x74,
            (byte)0xA2, (byte)0xBE, (byte)0x54, (byte)0x08,
            (byte)0xB4, (byte)0xFC, (byte)0x1D, (byte)0x13,
            (byte)0x84, (byte)0xBE, (byte)0x1C, (byte)0xC5,
            (byte)0x3E, (byte)0x43, (byte)0x14, (byte)0x6F,
            (byte)0xC0, (byte)0x3D, (byte)0xF4, (byte)0xDC,
            (byte)0x66, (byte)0x4E, (byte)0xF0, (byte)0x3E,
            (byte)0xD4, (byte)0xC6, (byte)0xE9, (byte)0x8D,
            (byte)0x7D, (byte)0xB9, (byte)0xDC, (byte)0x9F,
            (byte)0xBE, (byte)0x54, (byte)0x63, (byte)0x93,
            (byte)0x49, (byte)0x2F, (byte)0x6A, (byte)0xC3,
            (byte)0x34, (byte)0xC5, (byte)0xF7, (byte)0x76,
            (byte)0xE8, (byte)0xD5, (byte)0x5B, (byte)0xD9,
            (byte)0x41, (byte)0xCA, (byte)0x74, (byte)0x25,
            (byte)0x25, (byte)0x09, (byte)0xF4, (byte)0xD3,
            (byte)0x00, (byte)0x9F, (byte)0x7D, (byte)0xFB,
            (byte)0x3D, (byte)0xAB, (byte)0x87, (byte)0xF7,
            (byte)0xCE, (byte)0x42, (byte)0x0F, (byte)0x60,
            (byte)0xEB, (byte)0x03, (byte)0x47, (byte)0x98,
            (byte)0x0F, (byte)0xEB, (byte)0xA4, (byte)0x05,
            (byte)0xE2, (byte)0x58, (byte)0x8F, (byte)0x44,
            (byte)0x09, (byte)0xD3, (byte)0x66, (byte)0x1E,
            (byte)0x69, (byte)0x89, (byte)0xB7, (byte)0xEE,
            (byte)0x8B, (byte)0xA4, (byte)0x8E, (byte)0x05,
            (byte)0x2D, (byte)0x2E, (byte)0xB3, (byte)0x5A,
            (byte)0xAE, (byte)0xAB, (byte)0x80, (byte)0xD6,
            (byte)0x5C, (byte)0x93, (byte)0x40, (byte)0x91,
            (byte)0x53, (byte)0xE6, (byte)0x13, (byte)0xD5,
            (byte)0x2F, (byte)0x64, (byte)0xF0, (byte)0x68,
            (byte)0xD2, (byte)0x85, (byte)0x94, (byte)0xE5,
            (byte)0x2D, (byte)0x73, (byte)0x10, (byte)0x59,
            (byte)0x18, (byte)0xCD, (byte)0xED, (byte)0xBC,
            (byte)0x05, (byte)0x97, (byte)0xFD, (byte)0xE7,
            (byte)0x6F, (byte)0x5D, (byte)0x7C, (byte)0x46,
            (byte)0x28, (byte)0x5F, (byte)0xC2, (byte)0xB4,
            (byte)0x31, (byte)0xA5, (byte)0x2B, (byte)0x82,
            (byte)0xAB, (byte)0x32, (byte)0x49, (byte)0xA5,
            (byte)0xCD, (byte)0x91, (byte)0x37, (byte)0x97,
            (byte)0xA1, (byte)0x85, (byte)0x8F, (byte)0xBB,
            (byte)0x6E, (byte)0x1E, (byte)0x9F, (byte)0xFC,
            (byte)0x10, (byte)0x3B, (byte)0x8A, (byte)0xF6,
            (byte)0x9A, (byte)0x66, (byte)0xBD, (byte)0x75,
            (byte)0x4F, (byte)0x1D, (byte)0xBA, (byte)0x64,
            (byte)0x15, (byte)0xDD, (byte)0x9F, (byte)0x00,
            (byte)0x6C, (byte)0x2F, (byte)0x87, (byte)0x20,
            (byte)0x25, (byte)0xA2, (byte)0x09, (byte)0x9F,
            (byte)0x5D, (byte)0x64, (byte)0xC9, (byte)0xA8,
            (byte)0x32, (byte)0x59, (byte)0x90, (byte)0x1D,
            (byte)0x78, (byte)0xFE, (byte)0x5A, (byte)0xA2,
            (byte)0x1F, (byte)0x9B, (byte)0x22, (byte)0xBE,
            (byte)0x8F, (byte)0xEA, (byte)0x59, (byte)0x5B,
            (byte)0x96, (byte)0xE3, (byte)0x4A, (byte)0xB2,
            (byte)0x71, (byte)0x65, (byte)0xB7, (byte)0x3C,
            (byte)0xC6, (byte)0x1B, (byte)0xD6, (byte)0x80,
            (byte)0x90, (byte)0xD2, (byte)0xF2, (byte)0x6F,
            (byte)0xA2, (byte)0x68, (byte)0x53, (byte)0xC0,
            (byte)0x44, (byte)0xAF, (byte)0xD4, (byte)0x68,
            (byte)0x12, (byte)0xFF, (byte)0xB4, (byte)0x36,
            (byte)0x34, (byte)0x43, (byte)0xAC, (byte)0x1C,
    };
}
