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
 * @summary converted from VM Testbase nsk/jdi/ListeningConnector/accept/accept001.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test checks that debugger may establish connection with
 *     a debugee VM via com.sun.jdi.SocketListen connector.
 * COMMENTS
 *     Test updated to support launching debuggee VM from another JDK
 *     Test was fixed according to test bug:
 *     4778296 TEST_BUG: debuggee VM intemittently hangs after resuming
 *     - debuggee wrapper is used to handle VMStartEvent
 *     - output redirectors are incapsulated in the wrapper
 *     Test was fixed to provide early redirecting of debugee output.
 *     See bug:
 *     4905551 shmemBase_listen failed: Cannot create a file when that file already exists
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ListeningConnector.accept.accept001
 *        nsk.jdi.ListeningConnector.accept.accept001t
 * @run main/othervm
 *      nsk.jdi.ListeningConnector.accept.accept001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

