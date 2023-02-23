/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package jdk.jfr.internal.periodic;

/**
 * Lookup key that can safely be used in a {@code Map}.
 * <p>
 * {@code Runnable} objects can't be used with {@code LinkedHashMap} as it
 * invokes {@code hashCode} and {@code equals}, for example when resizing the
 * {@code Map}, possibly in a non-secure context.
 * <p>
 * {@code IdentityHashMap} can't be used as it will not preserve order.
 */
final class LookupKey {
    private final Object object;

    public LookupKey(Object object) {
        this.object = object;
    }

    public int hashCode() {
        return System.identityHashCode(object);
    }

    public boolean equals(Object that) {
        if (that instanceof LookupKey lookupKey) {
            return lookupKey.object == object;
        }
        return false;
    }
}