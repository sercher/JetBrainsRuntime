/*
 * Copyright 1998-2004 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

#ifndef _JAVA_PROPS_H
#define _JAVA_PROPS_H

#include <jni_util.h>

/* The preferred native type for storing text on the current OS */
#ifdef WIN32
#include <tchar.h>
typedef WCHAR nchar;
#else
typedef char nchar;
#endif

typedef struct {
    char *os_name;
    char *os_version;
    char *os_arch;

    nchar *tmp_dir;
    nchar *font_dir;
    nchar *user_dir;

    char *file_separator;
    char *path_separator;
    char *line_separator;

    nchar *user_name;
    nchar *user_home;

    char *language;
    char *country;
    char *variant;
    char *encoding;
    char *sun_jnu_encoding;
    char *timezone;

    char *printerJob;
    char *graphics_env;
    char *awt_toolkit;

    char *unicode_encoding;     /* The default endianness of unicode
                                    i.e. UnicodeBig or UnicodeLittle   */

    const char *cpu_isalist;    /* list of supported instruction sets */

    char *cpu_endian;           /* endianness of platform */

    char *data_model;           /* 32 or 64 bit data model */

    char *patch_level;          /* patches/service packs installed */

    char *desktop;              /* Desktop name. */

} java_props_t;

java_props_t *GetJavaProperties(JNIEnv *env);
jstring GetStringPlatform(JNIEnv *env, nchar* str);

#endif /* _JAVA_PROPS_H */
