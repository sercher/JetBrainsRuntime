/*
 * Copyright 2000-2023 JetBrains s.r.o.
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

package com.jetbrains.bootstrap;

import com.jetbrains.internal.JBRApi;

import java.lang.invoke.MethodHandles;
import java.util.Map;


/**
 * Bootstrap class, used to initialize {@linkplain JBRApi JBR API}.
 * @deprecated replaced by {@link com.jetbrains.exported.JBRApiSupport}
 */
@Deprecated
public class JBRApiBootstrap {
    private JBRApiBootstrap() {}

    /**
     * Old version of bootstrap method without metadata parameter.
     * @param outerLookup lookup context inside {@code jetbrains.api} module
     * @return implementation for {@link com.jetbrains.JBR.ServiceApi} interface
     */
    public static synchronized Object bootstrap(MethodHandles.Lookup outerLookup) {
        if (!JBRApi.ENABLED) return null;
        if (Boolean.getBoolean("jetbrains.runtime.api.verbose")) {
            System.out.println("JBR API bootstrap in compatibility mode: Object bootstrap(MethodHandles.Lookup)");
        }
        Class<?> apiInterface;
        try {
            apiInterface = outerLookup.findClass("com.jetbrains.JBR$ServiceApi");
        } catch (ClassNotFoundException | IllegalAccessException e) {
            throw new RuntimeException("Failed to retrieve JBR API metadata", e);
        }
        return com.jetbrains.exported.JBRApiSupport.bootstrap(apiInterface, null, null, null, Map.of(), m -> null);
    }

}
