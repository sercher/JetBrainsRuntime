/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

package pkg;

import java.util.*;

public class PublicChild extends PrivateParent
    implements PrivateInterface {

    public <T extends List, V> PublicChild methodOverriddenFromParent(
            char[] p1, int p2, T p3, V p4, List<String> p5)
    throws Exception {
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @param p1 {@inheritDoc}
     * @throws Exception {@inheritDoc}
     */
    public void methodInterface(int p1) throws Exception {
    }

    public void methodInterface2(int p1) throws Exception {
    }
}
