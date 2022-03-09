/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

import java.io.File;
import sun.security.action.GetPropertyAction;

class ClassLoaderHelper {
    private static final boolean hasDynamicLoaderCache;
    static {
        String osVersion = GetPropertyAction.privilegedGetProperty("os.version");
        // dynamic linker cache support on os.version >= 11.x
        int major = 11;
        int i = osVersion.indexOf('.');
        try {
            major = Integer.parseInt(i < 0 ? osVersion : osVersion.substring(0, i));
        } catch (NumberFormatException e) {}
        hasDynamicLoaderCache = major >= 11;
    }

    private ClassLoaderHelper() {}

    /**
     * Returns true if loading a native library only if
     * it's present on the file system.
     *
     * @implNote
     * On macOS 11.x or later which supports dynamic linker cache,
     * the dynamic library is not present on the filesystem.  The
     * library cannot determine if a dynamic library exists on a
     * given path or not and so this method returns false.
     */
    static boolean loadLibraryOnlyIfPresent() {
        return !hasDynamicLoaderCache;
    }

    /**
     * Indicates, whether PATH env variable is allowed to contain quoted entries.
     */
    static final boolean allowsQuotedPathElements = false;

    /**
     * Returns an alternate path name for the given file
     * such that if the original pathname did not exist, then the
     * file may be located at the alternate location.
     * For mac, this replaces the final .dylib suffix with .jnilib
     */
    static File mapAlternativeName(File lib) {
        String name = lib.toString();
        int index = name.lastIndexOf('.');
        if (index < 0) {
            return null;
        }
        return new File(name.substring(0, index) + ".jnilib");
    }
}
