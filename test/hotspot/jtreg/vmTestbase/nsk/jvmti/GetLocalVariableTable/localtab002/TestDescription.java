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
 * @summary converted from VM Testbase nsk/jvmti/GetLocalVariableTable/localtab002.
 * VM Testbase keywords: [quick, jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test exercises JVMTI function
 *         GetLocalVariableTable(method, entryCountPtr, tablePtr).
 *     The test checks if the function returns:
 *       - JVMTI_ERROR_INVALID_METHODID if method is null
 *       - JVMTI_ERROR_NULL_POINTER if entryCountPtr is null
 *       - JVMTI_ERROR_NULL_POINTER if tablePtr is null
 *       - JVMTI_ERROR_NATIVE_METHOD if method is native
 * COMMENTS
 *     Ported from JVMDI.
 *     Added check for native method.
 *
 * @library /vmTestbase
 *          /test/lib
 *
 * @comment make sure localtab002 is compiled with full debug info
 * @build nsk.jvmti.GetLocalVariableTable.localtab002
 * @clean nsk.jvmti.GetLocalVariableTable.localtab002
 * @compile -g:lines,source,vars ../localtab002.java
 *
 * @run main/othervm/native
 *      -agentlib:localtab002
 *      nsk.jvmti.GetLocalVariableTable.localtab002
 */

