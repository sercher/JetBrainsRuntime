/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6777487
 * @summary Tests private field access for EnumSet
 * @run main/othervm -Djava.security.manager=allow TestEnumSet
 * @author Sergey Malenkov
 */

import java.util.EnumSet;
import java.util.HashSet;

public final class TestEnumSet {
    private static final Object OBJECT = new Object();

    public static void main(String[] args) {
        TestEncoder.test(
                EnumSet.noneOf(Point.class),
                new HashSet() {
                    private final Object elementType = OBJECT;
                },
                OBJECT);
    }

    public enum Point { X, Y, Z }
}
