/*
 * Copyright (c) 2002, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8007464
 * @summary Negative regression test from odersky
 * @author odersky
 *
 * @compile/fail -source 7 BadTest4.java
 * @compile BadTest4.java
 */

class BadTest4 {

    interface I {}
    interface J {}
    static class C implements I, J {}
    static class D implements I, J {}

    interface Ord {}

    static class Main {

        static C c = new C();
        static D d = new D();

        static <B> List<B> nil() { return new List<B>(); }
        static <A, B extends A> A f(A x, B y) { return x; }
        static <A, B extends A> B g(List<A> x, List<B> y) { return y.head; }

        static <A> List<A> cons(A x, List<A> xs) { return xs.prepend(x); }
        static <A> Cell<A> makeCell(A x) { return new Cell<A>(x); }
        static <A> A id(A x) { return x; }

        static Integer i = new Integer(1);
        static Number n = i;

        public static void main(String[] args) {
            Number x = f(n, i);
            x = f(i, n);
            f(cons("abc", nil()), nil());
            f(nil(), cons("abc", nil()));
        }
    }
}
