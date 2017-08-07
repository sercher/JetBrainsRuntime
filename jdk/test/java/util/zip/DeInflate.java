/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7110149 8184306
 * @summary Test basic deflater & inflater functionality
 * @key randomness
 */

import java.io.*;
import java.util.*;
import java.util.zip.*;

public class DeInflate {

    static void checkStream(Deflater def, byte[] in, int len,
                            byte[] out1, byte[] out2, boolean nowrap)
        throws Throwable
    {
        Arrays.fill(out1, (byte)0);
        Arrays.fill(out2, (byte)0);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DeflaterOutputStream defos = new DeflaterOutputStream(baos, def)) {
            defos.write(in, 0, len);
        }
        out1 = baos.toByteArray();
        int m = out1.length;

        Inflater inf = new Inflater(nowrap);
        inf.setInput(out1, 0, m);
        int n = inf.inflate(out2);

        if (n != len ||
            !Arrays.equals(Arrays.copyOf(in, len), Arrays.copyOf(out2, len)) ||
            inf.inflate(out2) != 0) {
            System.out.printf("m=%d, n=%d, len=%d, eq=%b%n",
                              m, n, len, Arrays.equals(in, out2));
            throw new RuntimeException("De/inflater failed:" + def);
        }
    }

    static void check(Deflater def, byte[] in, int len,
                      byte[] out1, byte[] out2, boolean nowrap)
        throws Throwable
    {
        Arrays.fill(out1, (byte)0);
        Arrays.fill(out2, (byte)0);

        def.setInput(in, 0, len);
        def.finish();
        int m = def.deflate(out1);

        Inflater inf = new Inflater(nowrap);
        inf.setInput(out1, 0, m);
        int n = inf.inflate(out2);

        if (n != len ||
            !Arrays.equals(Arrays.copyOf(in, len), Arrays.copyOf(out2, len)) ||
            inf.inflate(out2) != 0) {
            System.out.printf("m=%d, n=%d, len=%d, eq=%b%n",
                              m, n, len, Arrays.equals(in, out2));
            throw new RuntimeException("De/inflater failed:" + def);
        }
    }

    private static Deflater newDeflater(int level, int strategy, boolean dowrap, byte[] tmp) {
        Deflater def = new Deflater(level, dowrap);
        if (strategy != Deflater.DEFAULT_STRATEGY) {
            def.setStrategy(strategy);
            // The first invocation after setLevel/Strategy()
            // with a different level/stragety returns 0, if
            // there is no need to flush out anything for the
            // previous setting/"data", this is tricky and
            // appears un-documented.
            def.deflate(tmp);
        }
        return def;
    }

    private static Deflater resetDeflater(Deflater def, int level, int strategy) {
        def.setLevel(level);
        def.setStrategy(strategy);
        def.reset();
        return def;
    }

    public static void main(String[] args) throws Throwable {

        byte[] dataIn = new byte[1024 * 512];
        new Random().nextBytes(dataIn);
        byte[] dataOut1 = new byte[dataIn.length + 1024];
        byte[] dataOut2 = new byte[dataIn.length];

        Deflater defNotWrap = new Deflater(Deflater.DEFAULT_COMPRESSION, false);
        Deflater defWrap = new Deflater(Deflater.DEFAULT_COMPRESSION, true);

        for (int level = Deflater.DEFAULT_COMPRESSION;
                 level <= Deflater.BEST_COMPRESSION; level++) {
            for (int strategy = Deflater.DEFAULT_STRATEGY;
                     strategy <= Deflater.HUFFMAN_ONLY; strategy++) {
                for (boolean dowrap : new boolean[] { false, true }) {
                    System.out.println("level:" + level +
                                     ", strategy: " + strategy +
                                     ", dowrap: " + dowrap);
                    for (int i = 0; i < 5; i++) {
                        int len = (i == 0)? dataIn.length
                                          : new Random().nextInt(dataIn.length);
                        // use a new deflater
                        Deflater def = newDeflater(level, strategy, dowrap, dataOut2);
                        check(def, dataIn, len, dataOut1, dataOut2, dowrap);

                        // reuse the deflater (with reset) and test on stream, which
                        // uses a "smaller" buffer (smaller than the overall data)
                        def = resetDeflater(dowrap ? defWrap : defNotWrap, level, strategy);
                        checkStream(def, dataIn, len, dataOut1, dataOut2, dowrap);
                    }
                }
            }
        }
    }
}
