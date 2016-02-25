/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 */
package java.net.http;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Implementation of HttpHeaders.
 */
class HttpHeadersImpl implements HttpHeaders1 {

    private final HashMap<String,List<String>> headers;
    private boolean isUnmodifiable = false;

    public HttpHeadersImpl() {
        headers = new HashMap<>();
    }

    /**
     * Replace all List<String> in headers with unmodifiable Lists. Call
     * this only after all headers are added. The headers HashMap
     * is wrapped with an unmodifiable HashMap in map()
     */
    @Override
    public void makeUnmodifiable() {
        if (isUnmodifiable)
            return;

        Set<String> keys = new HashSet<>(headers.keySet());
        for (String key : keys) {
            List<String> values = headers.remove(key);
            if (values != null) {
                headers.put(key, Collections.unmodifiableList(values));
            }
        }
        isUnmodifiable = true;
    }

    @Override
    public Optional<String> firstValue(String name) {
        List<String> l = headers.get(name);
        return Optional.ofNullable(l == null ? null : l.get(0));
    }

    @Override
    public List<String> allValues(String name) {
        return headers.get(name);
    }

    @Override
    public Map<String, List<String>> map() {
        return Collections.unmodifiableMap(headers);
    }

    Map<String, List<String>> directMap() {
        return headers;
    }

    // package private mutators

    public HttpHeadersImpl deepCopy() {
        HttpHeadersImpl h1 = new HttpHeadersImpl();
        HashMap<String,List<String>> headers1 = h1.headers;
        Set<String> keys = headers.keySet();
        for (String key : keys) {
            List<String> vals = headers.get(key);
            LinkedList<String> vals1 = new LinkedList<>(vals);
            headers1.put(key, vals1);
        }
        return h1;
    }

    private List<String> getOrCreate(String name) {
        List<String> l = headers.get(name);
        if (l == null) {
            l = new LinkedList<>();
            headers.put(name, l);
        }
        return l;
    }

    void addHeader(String name, String value) {
        List<String> l = getOrCreate(name);
        l.add(value);
    }

    void setHeader(String name, String value) {
        List<String> l = getOrCreate(name);
        l.clear();
        l.add(value);
    }

    @Override
    public Optional<Long> firstValueAsLong(String name) {
        List<String> l = headers.get(name);
        if (l == null) {
            return Optional.ofNullable(null);
        } else {
            String v = l.get(0);
            Long lv = Long.parseLong(v);
            return Optional.of(lv);
        }
    }

    void clear() {
        headers.clear();
    }
}
