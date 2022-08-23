/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8188055
 * @summary Basic functional test of Reference.refersTo.
 */

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.PhantomReference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

public class ReferenceRefersTo {
    private static final void fail(String msg) throws Exception {
        throw new RuntimeException(msg);
    }

    private static final <T extends Reference> void test(T ref,
                                                         Object expectedValue,
                                                         Object unexpectedValue,
                                                         String kind) throws Exception {
        if ((expectedValue != null) && ref.refersTo(null)) {
            fail(kind + "refers to null");
        }
        if (!ref.refersTo(expectedValue)) {
            fail(kind + " doesn't refer to expected value");
        }
        if (ref.refersTo(unexpectedValue)) {
            fail(kind + " refers to unexpected value");
        }
    }

    public static void main(String[] args) throws Exception {
        var queue = new ReferenceQueue<Object>();

        var obj0 = new Object();
        var obj1 = new Object();
        var obj2 = new Object();
        var obj3 = new Object();

        var pref = new PhantomReference(obj0, queue);
        var wref = new WeakReference(obj1);
        var sref = new SoftReference(obj2);

        test(pref, obj0, obj3, "phantom");
        test(wref, obj1, obj3, "weak");
        test(sref, obj2, obj3, "soft");

        pref.clear();
        wref.clear();
        sref.clear();

        test(pref, null, obj3, "phantom");
        test(wref, null, obj3, "weak");
        test(sref, null, obj3, "soft");
    }
}
