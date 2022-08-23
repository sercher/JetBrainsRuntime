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
 * @summary converted from VM Testbase nsk/jvmti/PopFrame/popframe001.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_caps, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test checks that a frame can be popped and no JVMTI events
 *     would be generated by the JVMTI function PopFrame().
 *     The test creates an instance of inner class popFrameCls and
 *     start it in a separate java thread. Then the test pops frame
 *     from the thread's stack of the class popFrameCls.
 * COMMENTS
 *     The test was changed due to the bug 4448675.
 *     Ported from JVMDI.
 *     Fixed according to 4912302 bug.
 *       - rearranged synchronization of tested thread
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native -agentlib:popframe001 nsk.jvmti.PopFrame.popframe001
 */

