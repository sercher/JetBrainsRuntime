/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr.event.gc.detailed;

/**
 * @test
 * @key jfr
 * @summary Test that events are created when an object is aged or promoted during a GC and the copying of the object requires a new PLAB or direct heap allocation
 * @requires vm.hasJFR
 *
 * @requires vm.gc == "Parallel" | vm.gc == null
 * @library /test/lib /test/jdk
 * @run main/othervm -Xmx32m -Xms32m -Xmn12m -XX:+UseParallelGC -XX:MaxTenuringThreshold=5 -XX:InitialTenuringThreshold=5 jdk.jfr.event.gc.detailed.TestPromotionEventWithParallelScavenge
 */
public class TestPromotionEventWithParallelScavenge {

    public static void main(String[] args) throws Throwable {
        PromotionEvent.test();
    }
}
