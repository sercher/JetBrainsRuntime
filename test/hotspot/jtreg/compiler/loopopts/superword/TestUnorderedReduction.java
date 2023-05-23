/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8302652
 * @summary Special test cases for PhaseIdealLoop::move_unordered_reduction_out_of_loop
 * @library /test/lib /
 * @run driver compiler.loopopts.superword.TestUnorderedReduction
 */

package compiler.loopopts.superword;

import compiler.lib.ir_framework.*;

public class TestUnorderedReduction {
    static final int RANGE = 1024;
    static final int ITER  = 10;

    public static void main(String[] args) {
        TestFramework.runWithFlags("-Xbatch",
                                   "-XX:CompileCommand=compileonly,compiler.loopopts.superword.TestUnorderedReduction::test*",
                                   "-XX:MaxVectorSize=16");
    }

    @Run(test = {"test1", "test2"})
    @Warmup(0)
    public void runTests() throws Exception {
        int[] data = new int[RANGE];

        init(data);
        for (int i = 0; i < ITER; i++) {
            int r1 = test1(data, i);
            int r2 = ref1(data, i);
            if (r1 != r2) {
                throw new RuntimeException("Wrong result test1: " + r1 + " != " + r2);
            }
        }

        for (int i = 0; i < ITER; i++) {
            int r1 = test2(data, i);
            int r2 = ref2(data, i);
            if (r1 != r2) {
                throw new RuntimeException("Wrong result test2: " + r1 + " != " + r2);
            }
        }
    }

    @Test
    @IR(counts = {IRNode.LOAD_VECTOR, "> 0",
                  IRNode.ADD_VI, "= 0",
                  IRNode.ADD_REDUCTION_VI, "> 0"}, // count can be high
        applyIfCPUFeatureOr = {"sse4.1", "true", "asimd", "true"})
    static int test1(int[] data, int sum) {
        // Vectorizes, but the UnorderedReduction cannot be moved out of the loop,
        // because we have a use inside the loop.
        int x = 0;
        for (int i = 0; i < RANGE; i+=8) {
            sum += 11 * data[i+0]; // vec 1 (16 bytes)
            sum += 11 * data[i+1];
            sum += 11 * data[i+2];
            sum += 11 * data[i+3];
            x = sum + i; // vec 1 reduction has more than 1 use
            sum += 11 * data[i+4]; // vec 2 (next 16 bytes)
            sum += 11 * data[i+5];
            sum += 11 * data[i+6];
            sum += 11 * data[i+7];
        }
        return sum + x;
    }

    static int ref1(int[] data, int sum) {
        int x = 0;
        for (int i = 0; i < RANGE; i+=8) {
            sum += 11 * data[i+0];
            sum += 11 * data[i+1];
            sum += 11 * data[i+2];
            sum += 11 * data[i+3];
            x = sum + i;
            sum += 11 * data[i+4];
            sum += 11 * data[i+5];
            sum += 11 * data[i+6];
            sum += 11 * data[i+7];
        }
        return sum + x;
    }

    @Test
    @IR(counts = {IRNode.LOAD_VECTOR, "> 0",
                  IRNode.ADD_VI, "> 0",
                  IRNode.ADD_REDUCTION_VI, "> 0",
                  IRNode.ADD_REDUCTION_VI, "<= 2"}, // count must be low
        applyIfCPUFeatureOr = {"sse4.1", "true", "asimd", "true"})
    static int test2(int[] data, int sum) {
        for (int i = 0; i < RANGE; i+=8) {
            // Vectorized, and UnorderedReduction moved outside loop.
            sum += 11 * data[i+0]; // vec 1
            sum += 11 * data[i+1];
            sum += 11 * data[i+2];
            sum += 11 * data[i+3];
            sum += 11 * data[i+4]; // vec 2
            sum += 11 * data[i+5];
            sum += 11 * data[i+6];
            sum += 11 * data[i+7];
        }
        return sum;
    }

    static int ref2(int[] data, int sum) {
        for (int i = 0; i < RANGE; i+=8) {
            sum += 11 * data[i+0];
            sum += 11 * data[i+1];
            sum += 11 * data[i+2];
            sum += 11 * data[i+3];
            sum += 11 * data[i+4];
            sum += 11 * data[i+5];
            sum += 11 * data[i+6];
            sum += 11 * data[i+7];
        }
        return sum;
    }


    static void init(int[] data) {
        for (int i = 0; i < RANGE; i++) {
            data[i] = i + 1;
        }
    }
}
