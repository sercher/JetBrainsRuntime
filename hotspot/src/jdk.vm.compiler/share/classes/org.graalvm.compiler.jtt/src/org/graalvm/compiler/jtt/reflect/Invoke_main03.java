/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.compiler.jtt.reflect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Test;

import org.graalvm.compiler.jtt.JTTTest;

/*
 */
public class Invoke_main03 extends JTTTest {

    public static class TestClass {
        public static void main(String[] args) {
            field = args[0];
        }
    }

    static String field;

    public static String test(String input) throws IllegalAccessException, InvocationTargetException {
        field = null;
        final String[] args = {input};
        for (Method m : TestClass.class.getDeclaredMethods()) {
            if ("main".equals(m.getName())) {
                m.invoke(null, new Object[]{args});
            }
        }
        return field;
    }

    @Test
    public void run0() throws Throwable {
        runTest("test", "test1");
    }

    @Test
    public void run1() throws Throwable {
        runTest("test", "test2");
    }

}
