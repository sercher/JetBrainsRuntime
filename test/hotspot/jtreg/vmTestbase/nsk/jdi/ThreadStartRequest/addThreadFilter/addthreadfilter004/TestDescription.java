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
 * @summary converted from VM Testbase nsk/jdi/ThreadStartRequest/addThreadFilter/addthreadfilter004.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the implementation of an object of the type
 *     ThreadStartRequest.
 *     The test checks up that a result of the method
 *     com.sun.jdi.ThreadStartRequest.addThreadFilter()
 *     complies with its spec:
 *     public void addThreadFilter(ThreadReference thread)
 *      Restricts the events generated by this request to those in the given thread.
 *      Parameters: thread - the thread to filter on.
 *      Throws: InvalidRequestStateException -
 *              if this request is currently enabled or has been deleted.
 *              Filters may be added only to disabled requests.
 *     The test checks up on the following assertion:
 *        Restricts the events generated by this request to those in the given thread.
 *     The cases to test include re-invocations of the method
 *     addThreadFilter() on the same ThreadStartRequest object.
 *     There are two ThreadStartRequests to check as follows:
 *     (1) For ThreadStartRequest2, both invocations are with different
 *         ThreadReferences restricting one ThreadStart event to two threads.
 *         The test expects no ThreadStart event will be received.
 *     (2) For ThreadStartRequest1, both invocations are with the same
 *         ThreadReference restricting one ThreadStart event to one thread.
 *         The test expects this ThreadStart event will be received.
 *     The test works as follows:
 *     The debugger program - nsk.jdi.ThreadStartRequest.addThreadFilter.addthreadfilter004;
 *     the debuggee program - nsk.jdi.ThreadStartRequest.addThreadFilter.addthreadfilter004a.
 *     Using nsk.jdi.share classes,
 *     the debugger gets the debuggee running on another JavaVM,
 *     creates the object debuggee.VM, and waits for VMStartEvent.
 *     Upon getting the debuggee VM started,
 *     the debugger calls corresponding debuggee.VM methods to get
 *     needed data and to perform checks.
 *     In case of error the test produces the return value 97 and
 *     a corresponding error message(s).
 *     Otherwise, the test is passed and produces
 *     the return value 95 and no message.
 * COMMENTS:
 *     4930911 TEST_BUG: filter_rt006 debuggee has a race
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ThreadStartRequest.addThreadFilter.addthreadfilter004
 *        nsk.jdi.ThreadStartRequest.addThreadFilter.addthreadfilter004a
 * @run main/othervm
 *      nsk.jdi.ThreadStartRequest.addThreadFilter.addthreadfilter004
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

