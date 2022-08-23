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
 * @summary converted from VM Testbase nsk/jvmti/SetEventNotificationMode/setnotif001.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_caps, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test exercises JVMTI function SetEventNotificationMode.
 *     Profiling agent setnotif001.c enables globally events:
 *       JVMTI_EVENT_SINGLE_STEP
 *       JVMTI_EVENT_BREAKPOINT
 *       JVMTI_EVENT_FRAME_POP
 *       JVMTI_EVENT_METHOD_ENTRY
 *       JVMTI_EVENT_METHOD_EXIT
 *       JVMTI_EVENT_FIELD_ACCESS
 *       JVMTI_EVENT_FIELD_MODIFICATION
 *       JVMTI_EVENT_EXCEPTION
 *       JVMTI_EVENT_EXCEPTION_CATCH
 *       JVMTI_EVENT_THREAD_END
 *       JVMTI_EVENT_THREAD_START
 *       JVMTI_EVENT_CLASS_LOAD
 *       JVMTI_EVENT_CLASS_PREPARE
 *     and checks if all they are sent after been enabled and are not
 *     sent before that.
 * COMMENTS
 *     Fixed according to the bug 4509016.
 *     Fixed according to the bug 4547922.
 *     Ported from JVMDI.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native -agentlib:setnotif001 nsk.jvmti.SetEventNotificationMode.setnotif001
 */

