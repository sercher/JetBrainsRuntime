/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.hotspot.igv.data;

import java.util.Arrays;
import java.util.HashSet;
import static org.junit.Assert.assertEquals;
import org.junit.*;

/**
 *
 * @author Thomas Wuerthinger
 */
public class GroupTest {

    public GroupTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {

    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getAllNodes method, of class Group.
     */
    @Test
    public void testGetAllNodes() {
        final Group g = new Group(null);
        final InputGraph graph1 = new InputGraph("1");
        final InputGraph graph2 = new InputGraph("2");
        g.addElement(graph1);
        g.addElement(graph2);
        graph1.addNode(new InputNode(1));
        graph1.addNode(new InputNode(2));
        graph2.addNode(new InputNode(2));
        graph2.addNode(new InputNode(3));
        assertEquals(g.getAllNodes(), new HashSet(Arrays.asList(1, 2, 3)));
    }
}
