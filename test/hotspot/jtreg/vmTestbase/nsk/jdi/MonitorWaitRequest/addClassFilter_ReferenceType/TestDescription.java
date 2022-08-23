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
 * @modules jdk.jdi/com.sun.tools.jdi:+open java.base/jdk.internal.misc:+open
 *
 * @summary converted from VM Testbase nsk/jdi/MonitorWaitRequest/addClassFilter_ReferenceType.
 * VM Testbase keywords: [quick, jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *         The test checks up that a result of the method com.sun.jdi.MonitorWaitRequest.addClassFilter(ReferenceType referenceType)
 *         complies with its spec:
 *         "Restricts the events generated by this request to be the preparation of the given reference type and any subtypes.
 *         An event will be generated for any prepared reference type that can be safely cast to the given reference type."
 *         Test also checks that received com.sun.jdi.MonitorWaitEvent complies with its spec and contains correct data.
 *         Test uses 2 event filters:
 *                 - add to request filter for class which has subclass (expect events for class and its subclasses)
 *                 - add to request filter for class without subclasses
 *         Test executes class nsk.share.jdi.ClassExclusionFilterTest which uses JDI events testing
 *         framework based on classes from package nsk.share.jdi.*.
 *         This framework uses following scenario:
 *                 - debugger VM forces debugge VM to create number of objects which should generate events during test
 *                 - if any event filters are used each generating event object is checked is this object accepted by all filters,
 *                 if object was accepted it should save information about all generated events and this information is available for debugger
 *                 - debuggee performs event generation and stop at breakpoint
 *                 - debugger reads data saved by debuggee's event generators and checks is only expected events was generated
 *         In addition to the main scenario tests using event filters also check following cases:
 *                 - attempt to add filter to enabled or deleted request throws 'InvalidRequestStateException'
 *                 - corresponding method EventRequestManager.xxxRequests() returns created event request
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.share.jdi.ClassFilterTest_ReferenceType
 *        nsk.share.jdi.JDIEventsDebuggee
 *        nsk.share.jdi.MonitorEventsDebuggee
 * @run main/othervm
 *      nsk.share.jdi.ClassFilterTest_ReferenceType
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 *      -eventType MONITOR_WAIT
 *      -debuggeeClassName nsk.share.jdi.MonitorEventsDebuggee
 *      -classPatterns nsk.share.jdi.MonitorWaitExecutor_1Subclass:nsk.share.jdi.MonitorWaitExecutor_2Subclass
 */

