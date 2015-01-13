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

import com.oracle.java.testlibrary.Asserts;
import java.lang.management.MemoryPoolMXBean;
import sun.hotspot.code.BlobType;

/*
 * @test UsageThresholdExceededTest
 * @library /testlibrary /../../test/lib
 * @build UsageThresholdExceededTest
 * @run main ClassFileInstaller sun.hotspot.WhiteBox
 *     sun.hotspot.WhiteBox$WhiteBoxPermission
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *     -XX:+WhiteBoxAPI -XX:+SegmentedCodeCache -XX:-UseCodeCacheFlushing
 *     -XX:-MethodFlushing -XX:CompileCommand=compileonly,null::*
 *     UsageThresholdExceededTest
 * @summary verifying that getUsageThresholdCount() returns correct value
 *     after threshold has been hit
 */
public class UsageThresholdExceededTest {

    protected final int iterations;
    private final BlobType btype;

    public UsageThresholdExceededTest(BlobType btype, int iterations) {
        this.btype = btype;
        this.iterations = iterations;
    }

    public static void main(String[] args) {
        int iterationsCount =
            Integer.getInteger("com.oracle.java.testlibrary.iterations", 1);
        for (BlobType btype : BlobType.getAvailable()) {
            if (CodeCacheUtils.isCodeHeapPredictable(btype)) {
                new UsageThresholdExceededTest(btype, iterationsCount)
                        .runTest();
            }
        }
    }

    protected void runTest() {
        MemoryPoolMXBean bean = btype.getMemoryPool();
        long oldValue = bean.getUsageThresholdCount();
        for (int i = 0; i < iterations; i++) {
            CodeCacheUtils.hitUsageThreshold(bean, btype);
        }
        Asserts.assertEQ(bean.getUsageThresholdCount(), oldValue + iterations,
                "Unexpected threshold usage count");
        System.out.printf("INFO: Scenario finished successfully for %s%n",
                bean.getName());
    }
}
