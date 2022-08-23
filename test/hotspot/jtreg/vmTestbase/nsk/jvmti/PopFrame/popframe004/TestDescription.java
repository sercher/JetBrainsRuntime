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
 * @summary converted from VM Testbase nsk/jvmti/PopFrame/popframe004.
 * VM Testbase keywords: [quick, jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test checks that a method's frame can not be popped by
 *     the JVMTI function PopFrame():
 *      - with intermediate native frames, and a thread, from which
 *        the PopFrame() was called, is different than the current thread;
 *      - from the current thread with intermediate native frames;
 *      - no JVMTI events will be generated by the function PopFrame()
 * COMMENTS
 *     The test was changed due to the bug 4448675.
 *     Fixed according to the 4528173 bug:
 *       for now the test checks that a native frame may not be poped.
 *     Ported from JVMDI.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native -agentlib:popframe004 nsk.jvmti.PopFrame.popframe004
 */

