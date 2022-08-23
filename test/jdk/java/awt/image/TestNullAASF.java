/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
/**
 * @test
 * @bug 6211242
 * @summary Verifies IAE is thrown if 0 is passed to
 *          AreaAveragingScaleFilter(width,height) constructor.
 */
import java.awt.image.AreaAveragingScaleFilter;

public class TestNullAASF {
    public static void main(String[] args) {
        AreaAveragingScaleFilter filter = null;
        try {
            filter = new AreaAveragingScaleFilter(0, Integer.MAX_VALUE);
            System.out.println("result: false");
            throw new RuntimeException("IAE expected for width=0");
        } catch (IllegalArgumentException e) {
            System.out.println("result (iae): true");
        }
        try {
            filter = new AreaAveragingScaleFilter(Integer.MAX_VALUE, 0);
            System.out.println("result: false");
            throw new RuntimeException("IAE expected for height=0");
        } catch (IllegalArgumentException e) {
            System.out.println("result (iae): true");
        }
    }
}
