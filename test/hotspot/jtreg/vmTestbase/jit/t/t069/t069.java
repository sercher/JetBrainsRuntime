/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @summary converted from VM Testbase jit/t/t069.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t069.t069
 */

package jit.t.t069;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

// Dup, dup_x2, and dup2_x2.

public class t069
{
    public static final GoldChecker goldChecker = new GoldChecker( "t069" );

    public static void main(String[] argv)
    {
        int ia[] = new int[2];
        long la[] = new long[2];
        int i, j;

        ia[0] = ia[1] = 39;
        la[1] = la[0] = 42;
        t069.goldChecker.println(ia[0] + " " + ia[1] + " " + la[1] + " " + la[0]);

        i = 0;
        j = 1;

        ia[i] = ia[j] = 39;
        la[j] = la[i] = 42;
        t069.goldChecker.println(ia[i] + " " + ia[j] + " " + la[j] + " " + la[i]);
        t069.goldChecker.check();
    }
}
