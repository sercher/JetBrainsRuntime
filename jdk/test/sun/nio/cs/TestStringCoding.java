/*
 * Copyright 2000-2009 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
   @bug 6636323 6636319
   @summary Test if StringCoding and NIO result have the same de/encoding result
 * @run main/timeout=2000 TestStringCoding
 */

import java.util.*;
import java.nio.*;
import java.nio.charset.*;

public class TestStringCoding {
    public static void main(String[] args) throws Throwable {

        for (Boolean hasSM: new boolean[] { false, true }) {
            if (hasSM)
                System.setSecurityManager(new PermissiveSecurityManger());
            for (Charset cs:  Charset.availableCharsets().values()) {
                if ("ISO-2022-CN".equals(cs.name()) ||
                    "x-COMPOUND_TEXT".equals(cs.name()) ||
                    "x-JISAutoDetect".equals(cs.name()))
                    continue;
                System.out.printf("Testing(sm=%b) " + cs.name() + "....", hasSM);
                // full bmp first
                char[] bmpCA = new char[0x10000];
                for (int i = 0; i < 0x10000; i++) {
                     bmpCA[i] = (char)i;
                }
                byte[] sbBA = new byte[0x100];
                for (int i = 0; i < 0x100; i++) {
                    sbBA[i] = (byte)i;
                }
                test(cs, bmpCA, sbBA);
                // "randomed" sizes
                Random rnd = new Random();
                for (int i = 0; i < 10; i++) {
                    int clen = rnd.nextInt(0x10000);
                    int blen = rnd.nextInt(0x100);
                    //System.out.printf("    blen=%d, clen=%d%n", blen, clen);
                    test(cs, Arrays.copyOf(bmpCA, clen), Arrays.copyOf(sbBA, blen));
                    //add a pair of surrogates
                    int pos = clen / 2;
                    if ((pos + 1) < blen) {
                        bmpCA[pos] = '\uD800';
                        bmpCA[pos+1] = '\uDC00';
                    }
                    test(cs, Arrays.copyOf(bmpCA, clen), Arrays.copyOf(sbBA, blen));
                }
                System.out.println("done!");
            }
        }
    }

    static void test(Charset cs, char[] bmpCA, byte[] sbBA) throws Throwable {
        String bmpStr = new String(bmpCA);
        CharsetDecoder dec = cs.newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE);
        CharsetEncoder enc = cs.newEncoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE);

        //getBytes(csn);
        byte[] baSC = bmpStr.getBytes(cs.name());
        ByteBuffer bf = enc.reset().encode(CharBuffer.wrap(bmpCA));
        byte[] baNIO = new byte[bf.limit()];
        bf.get(baNIO, 0, baNIO.length);
        if (!Arrays.equals(baSC, baNIO))
            throw new RuntimeException("getBytes(csn) failed  -> " + cs.name());

        //getBytes(cs);
        baSC = bmpStr.getBytes(cs);
        if (!Arrays.equals(baSC, baNIO))
            throw new RuntimeException("getBytes(cs) failed  -> " + cs.name());

        //new String(csn);
        String strSC = new String(sbBA, cs.name());
        String strNIO = dec.reset().decode(ByteBuffer.wrap(sbBA)).toString();
        if(!strNIO.equals(strSC))
            throw new RuntimeException("new String(csn) failed  -> " + cs.name());

        //new String(cs);
        strSC = new String(sbBA, cs);
        if (!strNIO.equals(strSC))
            throw new RuntimeException("new String(cs) failed  -> " + cs.name());

        //encode unmappable surrogates
        if (enc instanceof sun.nio.cs.ArrayEncoder &&
            cs.contains(Charset.forName("ASCII"))) {
            enc.replaceWith(new byte[] { (byte)'A'});
            sun.nio.cs.ArrayEncoder cae = (sun.nio.cs.ArrayEncoder)enc;

            String str = "ab\uD800\uDC00\uD800\uDC00cd";
            byte[] ba = new byte[str.length() - 2];
            int n = cae.encode(str.toCharArray(), 0, str.length(), ba);
            if (n != 6 || !"abAAcd".equals(new String(ba, cs.name())))
                throw new RuntimeException("encode1(surrogates) failed  -> "
                                           + cs.name());

            ba = new byte[str.length()];
            n = cae.encode(str.toCharArray(), 0, str.length(), ba);
            if (n != 6 || !"abAAcd".equals(new String(ba, 0, n,
                                                     cs.name())))
                throw new RuntimeException("encode2(surrogates) failed  -> "
                                           + cs.name());
            str = "ab\uD800B\uDC00Bcd";
            ba = new byte[str.length()];
            n = cae.encode(str.toCharArray(), 0, str.length(), ba);
            if (n != 8 || !"abABABcd".equals(new String(ba, 0, n,
                                                       cs.name())))
                throw new RuntimeException("encode3(surrogates) failed  -> "
                                           + cs.name());

            ba = new byte[str.length() - 1];
            n = cae.encode(str.toCharArray(), 0, str.length(), ba);
            if (n != 7 || !"abABABc".equals(new String(ba, 0, n,
                                                      cs.name())))
                throw new RuntimeException("encode4(surrogates) failed  -> "
                                           + cs.name());
        }

    }

    static class PermissiveSecurityManger extends SecurityManager {
        @Override public void checkPermission(java.security.Permission p) {}
    }
}
