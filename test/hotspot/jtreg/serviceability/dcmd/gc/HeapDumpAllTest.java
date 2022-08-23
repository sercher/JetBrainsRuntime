/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;

import jdk.test.lib.dcmd.CommandExecutor;

/*
 * @test
 * @summary Test of diagnostic command GC.heap_dump -all=true
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.compiler
 *          java.management
 *          jdk.internal.jvmstat/sun.jvmstat.monitor
 * @run testng/timeout=240 HeapDumpAllTest
 */
public class HeapDumpAllTest extends HeapDumpTest {
    public HeapDumpAllTest() {
        super();
        heapDumpArgs = "-all=true";
    }

    @Override
    public void run(CommandExecutor executor, boolean overwrite) throws IOException {
        // Trigger gc by hand, so the created heap dump isnt't too large and
        // takes too long to parse.
        System.gc();
        super.run(executor, overwrite);
    }

    /* See HeapDumpTest for test cases */
}
