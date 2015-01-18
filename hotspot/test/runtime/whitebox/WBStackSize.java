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
 * @test WBStackSize
 * @summary verify that whitebox functions getThreadFullStackSize() and getThreadRemainingStackSize are working
 * @library /testlibrary /../../test/lib
 * @build WBStackSize
 * @run main ClassFileInstaller sun.hotspot.WhiteBox
 *                              sun.hotspot.WhiteBox$WhiteBoxPermission
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xss512k WBStackSize
 */

/*
 * The test may product a false failure if too big StackYellowPages/StackRedPages/ShackShadowPages
 * VM options are specified. The proper test would retrieve the page size from VM and account for these options
 * instead of check below:
 *     Math.abs(actualStackSize - configStackSize) > configStackSize * 0.1
 *
 * Please file a test bug, if this is a problem.
 */

import sun.hotspot.WhiteBox;

public class WBStackSize {

    static final long K = 1024;

    static final long MIN_STACK_SIZE = 8 * K;
    static final long MAX_STACK_SIZE_ALLOCATED_IN_MAIN = 150 * K; // current value is about 130k on 64-bit platforms

    static final WhiteBox wb = WhiteBox.getWhiteBox();

    static long stackSizeOnOverflow = -1;

    static int eatAllStack() {
        return eatAllStack() * 2;
    }

    static void testStackOverflow() {

        stackSizeOnOverflow = wb.getThreadRemainingStackSize();

        if (stackSizeOnOverflow > MIN_STACK_SIZE) {

            try {
                testStackOverflow();
            } catch (StackOverflowError e) {
                // We caught SOE too early. The error will be reported in main()
            }

        } else {

            try {
                eatAllStack();
                throw new RuntimeException("Haven't caught StackOverflowError at all");
            } catch (StackOverflowError e) {
                // OK: we caught the anticipated error
            }
        }
    }

    public static void main(String[] args) {
        long configStackSize = wb.getIntxVMFlag("ThreadStackSize") * K;
        System.out.println("ThreadStackSize VM option: " + configStackSize);

        long stackProtectionSize = wb.getIntxVMFlag("StackShadowPages") * wb.getVMPageSize();
        System.out.println("Size of protected shadow pages: " + stackProtectionSize);

        long actualStackSize = wb.getThreadStackSize();
        System.out.println("Full stack size: " + actualStackSize);

        if (Math.abs(actualStackSize - configStackSize) > configStackSize * 0.1) {
            throw new RuntimeException("getThreadFullStackSize value [" + actualStackSize
                                     + "] should be within 90%..110% of the value returned by HotSpotDiagnosticMXBean");
        }

        long remainingStackSize = wb.getThreadRemainingStackSize();
        System.out.println("Remaining stack size in main(): " + remainingStackSize);

        // Up to 150k can be already allocated by VM and some space is used for stack protection.
        long spaceAlreadyOccupied = MAX_STACK_SIZE_ALLOCATED_IN_MAIN + stackProtectionSize;

        if (remainingStackSize > configStackSize
            || (configStackSize > spaceAlreadyOccupied
                && remainingStackSize < configStackSize - spaceAlreadyOccupied)) {

            throw new RuntimeException("getThreadRemainingStackSize value [" + remainingStackSize
                                     + "] should be at least ThreadStackSize value [" + configStackSize + "] minus ["
                                     + spaceAlreadyOccupied + "]");
        }

        testStackOverflow();

        if (stackSizeOnOverflow > MIN_STACK_SIZE) {
            throw new RuntimeException("Caught StackOverflowError too early: when there were "
                                     + stackSizeOnOverflow + " bytes in stack");
        } else if (stackSizeOnOverflow < 0) {
            throw new RuntimeException("Internal test error: stackRemainingSize < 0");
        } else {
            System.out.println("Caught StackOverflowError as expected");
        }
    }
}
