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

import sha.predicate.IntrinsicPredicates;

/**
 * @test
 * @bug 8035968
 * @summary Verify that SHA-1 multi block intrinsic is actually used.
 * @library /testlibrary /testlibrary/whitebox /compiler/testlibrary ../
 * @build TestSHA intrinsics.Verifier TestSHA1MultiBlockIntrinsics
 * @run main ClassFileInstaller sun.hotspot.WhiteBox
 *                              sun.hotspot.WhiteBox$WhiteBoxPermission
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI -Xbatch -XX:CompileThreshold=500
 *                   -XX:Tier4InvocationThreshold=500
 *                   -XX:+LogCompilation -XX:LogFile=positive.log
 *                   -XX:CompileOnly=sun/security/provider/DigestBase
 *                   -XX:CompileOnly=sun/security/provider/SHA
 *                   -XX:+UseSHA1Intrinsics -XX:-UseSHA256Intrinsics
 *                   -XX:-UseSHA512Intrinsics
 *                   -Dalgorithm=SHA-1 TestSHA1MultiBlockIntrinsics
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI -Xbatch -XX:CompileThreshold=500
 *                   -XX:Tier4InvocationThreshold=500
 *                   -XX:+LogCompilation -XX:LogFile=positive_def.log
 *                   -XX:CompileOnly=sun/security/provider/DigestBase
 *                   -XX:CompileOnly=sun/security/provider/SHA
 *                   -XX:+UseSHA1Intrinsics -Dalgorithm=SHA-1
 *                   TestSHA1MultiBlockIntrinsics
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI -Xbatch -XX:CompileThreshold=500
 *                   -XX:Tier4InvocationThreshold=500
 *                   -XX:+LogCompilation -XX:LogFile=negative.log
 *                   -XX:CompileOnly=sun/security/provider/DigestBase
 *                   -XX:CompileOnly=sun/security/provider/SHA -XX:-UseSHA
 *                   -Dalgorithm=SHA-1 TestSHA1MultiBlockIntrinsics
 * @run main/othervm -DverificationStrategy=VERIFY_INTRINSIC_USAGE
 *                   intrinsics.Verifier positive.log positive_def.log
 *                   negative.log
 */
public class TestSHA1MultiBlockIntrinsics {
    public static void main(String args[]) throws Exception {
        new SHASanityTestBase(IntrinsicPredicates.SHA1_INTRINSICS_AVAILABLE,
                SHASanityTestBase.MB_INTRINSIC_ID).test();
    }
}
