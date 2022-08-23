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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/allocation/AP10/ap10t001.
 * VM Testbase keywords: [quick, jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *   The test checks if the following functions
 *        SetEnvironmentLocalStorage
 *        GetEnvironmentLocalStorage
 *        GetCurrentThreadCpuTimerInfo
 *        GetCurrentThreadCpuTime
 *        GetTimerInfo
 *        GetTime
 *   can be used in the callbacks of ObjectFree,
 *   GarbageCollectionStart, GarbageCollectionFinish events.
 *   Usage of these functions is allowed by the JVMTI spec.
 * COMMENTS
 *   The test implements AP10 scenario of test plan for Allocation
 *   Profiling. Other test cases of this scenario are implemented as
 *   functional tests for heap iteration functions.
 *   Fixed the 4968019, 5005389, 5006885 bugs.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native
 *      -agentlib:ap10t001=-waittime=5
 *      nsk.jvmti.scenarios.allocation.AP10.ap10t001
 */

