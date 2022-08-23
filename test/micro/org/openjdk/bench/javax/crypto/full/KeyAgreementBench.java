/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.javax.crypto.full;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;

import java.security.*;
import javax.crypto.KeyAgreement;

public abstract class KeyAgreementBench extends CryptoBase {

    @Param({})
    private String kpgAlgorithm;

    @Param({})
    private String algorithm;

    @Param({})
    private int keyLength;


    private KeyAgreement keyAgreement;
    private PrivateKey privKey;
    private PublicKey pubKey;

    @Setup
    public void setup() throws NoSuchAlgorithmException {
        setupProvider();
        KeyPairGenerator generator = (prov == null) ?
            KeyPairGenerator.getInstance(kpgAlgorithm) :
            KeyPairGenerator.getInstance(kpgAlgorithm, prov);
        generator.initialize(keyLength);
        KeyPair kpA = generator.generateKeyPair();
        privKey = kpA.getPrivate();
        KeyPair kpB = generator.generateKeyPair();
        pubKey = kpB.getPublic();

        keyAgreement = (prov == null) ?
            KeyAgreement.getInstance(algorithm) :
            KeyAgreement.getInstance(algorithm, prov);
    }

    @Benchmark
    public byte[] generateSecret() throws InvalidKeyException {
        keyAgreement.init(privKey);
        keyAgreement.doPhase(pubKey, true);
        return keyAgreement.generateSecret();
    }

    public static class DiffieHellman extends KeyAgreementBench {

        @Param({"DiffieHellman"})
        private String kpgAlgorithm;

        @Param({"DiffieHellman"})
        private String algorithm;

        @Param({"2048"})
        private int keyLength;

    }

    public static class EC extends KeyAgreementBench {

        @Param({"EC"})
        private String kpgAlgorithm;

        @Param({"ECDH"})
        private String algorithm;

        @Param({"256", "384", "521"})
        private int keyLength;

    }

    public static class XDH extends KeyAgreementBench {

        @Param({"XDH"})
        private String kpgAlgorithm;

        @Param({"XDH"})
        private String algorithm;

        @Param({"255", "448"})
        private int keyLength;

    }

}

