/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @test Test2GbHeap
 * @bug 8031686
 * @summary Regression test to ensure we can start G1 with 2gb heap.
 * Skip test on 32 bit Windows: it typically does not support the many and large virtual memory reservations needed.
 * @requires (vm.gc == "G1" | vm.gc == "null")
 * @requires !((sun.arch.data.model == "32") & (os.family == "windows"))
 * @key gc
 * @key regression
 * @library /testlibrary
 * @modules java.base/sun.misc
 *          java.management
 */

import java.util.ArrayList;

import jdk.test.lib.OutputAnalyzer;
import jdk.test.lib.ProcessTools;

public class Test2GbHeap {
  public static void main(String[] args) throws Exception {
    ArrayList<String> testArguments = new ArrayList<String>();

    testArguments.add("-XX:+UseG1GC");
    testArguments.add("-Xmx2g");
    testArguments.add("-version");

    ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(testArguments.toArray(new String[0]));

    OutputAnalyzer output = new OutputAnalyzer(pb.start());
    output.shouldHaveExitValue(0);
  }
}
