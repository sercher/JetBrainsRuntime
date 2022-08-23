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
 * @summary converted from VM Testbase nsk/jvmti/GetObjectMonitorUsage/objmonusage005.
 * VM Testbase keywords: [quick, jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test checks if JVMTI function GetObjectMonitorUsage does not
 *     return JVMTI_ERROR_THREAD_NOT_SUSPENDED error code if monitor's
 *     holding thread is not suspended.
 *     Failing criteria for the test are:
 *       - failures of used JVMTI functions.
 * COMMENTS
 *     Ported from JVMDI test nsk/jvmdi/GetMonitorInfo/getmoninfo005.
 *     Fixed according to 4925857 bug:
 *       - rearranged synchronization of tested thread
 *       - enhanced descripton
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native -agentlib:objmonusage005 nsk.jvmti.GetObjectMonitorUsage.objmonusage005
 */

