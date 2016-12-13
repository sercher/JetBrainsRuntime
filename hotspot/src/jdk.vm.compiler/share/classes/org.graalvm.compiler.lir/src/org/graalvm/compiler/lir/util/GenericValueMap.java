/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.compiler.lir.util;

import java.util.Map;

import org.graalvm.compiler.core.common.CollectionsFactory;

import jdk.vm.ci.meta.Value;

public final class GenericValueMap<T> extends ValueMap<Value, T> {

    private final Map<Value, T> data;

    public GenericValueMap() {
        data = CollectionsFactory.newMap();
    }

    @Override
    public T get(Value value) {
        return data.get(value);
    }

    @Override
    public void remove(Value value) {
        data.remove(value);
    }

    @Override
    public void put(Value value, T object) {
        data.put(value, object);
    }

}
