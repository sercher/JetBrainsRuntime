/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package gc.stress.gcbasher;

import java.io.IOException;

/*
 * @test TestGCBasherWithAllocateHeapAt
 * @key stress
 * @library /
 * @requires vm.gc.G1
 * @requires vm.flavor == "server" & !vm.emulatedClient & os.family != "aix"
 * @summary Stress Java heap allocation with AllocateHeapAt flag using GC basher.
 * @run main/othervm/timeout=500 -Xlog:gc*=info -Xmx256m -server -XX:+UseG1GC -XX:AllocateHeapAt=. gc.stress.gcbasher.TestGCBasherWithAllocateHeapAt 120000
 */
public class TestGCBasherWithAllocateHeapAt {
    public static void main(String[] args) throws IOException {
        TestGCBasher.main(args);
    }
}
