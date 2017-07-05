/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @summary run NMT baseline, allocate memory and verify output from detail.diff
 * @key nmt jcmd
 * @library /testlibrary /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build JcmdDetailDiff
 * @run main ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -XX:NativeMemoryTracking=detail JcmdDetailDiff
 */

import jdk.test.lib.*;

import sun.hotspot.WhiteBox;

public class JcmdDetailDiff {

    public static WhiteBox wb = WhiteBox.getWhiteBox();

    public static void main(String args[]) throws Exception {
        ProcessBuilder pb = new ProcessBuilder();
        OutputAnalyzer output;
        // Grab my own PID
        String pid = Long.toString(ProcessTools.getProcessId());

        long commitSize = 128 * 1024;
        long reserveSize = 256 * 1024;
        long addr;

        // Run 'jcmd <pid> VM.native_memory baseline=true'
        pb.command(new String[] { JDKToolFinder.getJDKTool("jcmd"), pid, "VM.native_memory", "baseline=true"});
        pb.start().waitFor();

        output = new OutputAnalyzer(pb.start());
        output.shouldContain("Baseline succeeded");

        addr = wb.NMTReserveMemory(reserveSize);
        pb.command(new String[] { JDKToolFinder.getJDKTool("jcmd"), pid, "VM.native_memory", "detail.diff", "scale=KB"});

        output = new OutputAnalyzer(pb.start());
        output.shouldContain("Test (reserved=256KB +256KB, committed=0KB)");

        wb.NMTCommitMemory(addr, commitSize);
        pb.command(new String[] { JDKToolFinder.getJDKTool("jcmd"), pid, "VM.native_memory", "detail.diff", "scale=KB"});

        output = new OutputAnalyzer(pb.start());
        output.shouldContain("Test (reserved=256KB +256KB, committed=128KB +128KB)");

        wb.NMTUncommitMemory(addr, commitSize);
        pb.command(new String[] { JDKToolFinder.getJDKTool("jcmd"), pid, "VM.native_memory", "detail.diff", "scale=KB"});

        output = new OutputAnalyzer(pb.start());
        output.shouldContain("Test (reserved=256KB +256KB, committed=0KB)");

        wb.NMTReleaseMemory(addr, reserveSize);
        pb.command(new String[] { JDKToolFinder.getJDKTool("jcmd"), pid, "VM.native_memory", "detail.diff", "scale=KB"});

        output = new OutputAnalyzer(pb.start());
        output.shouldNotContain("Test (reserved=");
    }
}
