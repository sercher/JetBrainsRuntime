/*
 * Copyright (c) 2006, 2020, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 6463990
 * @summary java.net.URLDecoder.decode accepts negative hex numbers %-1 to %-f
 */

import java.net.URLDecoder;

public class B6463990 {
    public static void main(String[] args) {
        boolean except = false;
        try {
            String s = URLDecoder.decode("%-1", "iso-8859-1");
            System.out.println((int) s.charAt(0));
        } catch (Exception e) {
            except = true;
        }
        if (!except)
            throw new RuntimeException("IllegalArgumentException not thrown!");
    }
}
