/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @summary converted from VM Testbase nsk/jvmti/GetLineNumberTable/linetab003.
 * VM Testbase keywords: [quick, jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test exercises JVMTI function GetLineNumberTable.
 *     The test checks if the function returns JVMTI_ERROR_ABSENT_INFORMATION
 *     if class information does not include line numbers.
 * COMMENTS
 *     Ported from JVMDI.
 *
 * @library /vmTestbase
 *          /test/lib
 *
 * @comment make sure linetab003 is compiled with no debug info
 * @build nsk.jvmti.GetLineNumberTable.linetab003
 * @clean nsk.jvmti.GetLineNumberTable.linetab003
 * @compile -g:none ../linetab003.java
 *
 * @run main/othervm/native -agentlib:linetab003 nsk.jvmti.GetLineNumberTable.linetab003
 */

