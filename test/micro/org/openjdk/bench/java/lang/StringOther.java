/*
 * Copyright (c) 2014, 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.java.lang;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(3)
public class StringOther {

    private String testString;
    private Random rnd;

    @Setup
    public void setup() {
        testString = "Idealism is what precedes experience; cynicism is what follows.";
        rnd = new Random();
    }

    @Benchmark
    public void charAt(Blackhole bh) {
        for (int i = 0; i < testString.length(); i++) {
            bh.consume(testString.charAt(i));
        }
    }

    /**
     * Creates (hopefully) unique Strings and internizes them, creating a zillion forgettable strings in the JVMs string
     * pool.
     * <p/>
     * This will test 1.) The data structure/whatever for getting and adding Strings to intern table. 2.) The
     * intern-caches (java) behaviour on negative lookup (the string is new) 3.) GC's handling of weak handles. Since
     * every gc we must process and pretty much kill a zillion interned strings that are now not referenced anymore, the
     * majority of GC time will be spent in handle processing. So we get a picture of how well the pathological case of
     * this goes.
     */
    @Benchmark
    public String internUnique() {
        return String.valueOf(rnd.nextInt()).intern();
    }

}
