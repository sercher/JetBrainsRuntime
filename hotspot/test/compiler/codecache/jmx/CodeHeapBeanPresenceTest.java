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
import java.util.EnumSet;
import sun.hotspot.code.BlobType;

/**
 * @test CodeHeapBeanPresenceTest
 * @library /testlibrary /../../test/lib
 * @build CodeHeapBeanPresenceTest
 * @run main ClassFileInstaller sun.hotspot.WhiteBox
 *     sun.hotspot.WhiteBox$WhiteBoxPermission
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *     -XX:+WhiteBoxAPI -XX:-SegmentedCodeCache CodeHeapBeanPresenceTest
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *     -XX:+WhiteBoxAPI -XX:+SegmentedCodeCache CodeHeapBeanPresenceTest
 * @summary verify CodeHeap bean presence
 */
public class CodeHeapBeanPresenceTest {

    public static void main(String args[]) {
        EnumSet<BlobType> shouldBeAvailable = BlobType.getAvailable();
        EnumSet<BlobType> shouldNotBeAvailable
                = EnumSet.complementOf(shouldBeAvailable);
        for (BlobType btype : shouldBeAvailable) {
            Asserts.assertNotNull(btype.getMemoryPool(),
                    "Can't find memory pool for " + btype.name());
        }
        for (BlobType btype : shouldNotBeAvailable) {
            Asserts.assertNull(btype.getMemoryPool(),
                    "Memory pool unexpected for " + btype.name());
        }
    }
}
