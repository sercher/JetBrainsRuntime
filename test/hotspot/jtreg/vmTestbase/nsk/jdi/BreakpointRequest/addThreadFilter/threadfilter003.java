/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.BreakpointRequest.addThreadFilter;

import nsk.share.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.io.*;

/**
 * The test for the implementation of an object of the type     <BR>
 * BreakpointRequest.                                           <BR>
 *                                                              <BR>
 * The test checks that results of the method                   <BR>
 * <code>com.sun.jdi.BreakpointRequest.addThreadFilter()</code> <BR>
 * complies with its spec.                                      <BR>
 * <BR>
 * The test checks up on the following assertion:               <BR>
 *     Restricts the events generated by this request           <BR>
 *     to those in the given thread.                            <BR>
 *  The cases to test include three thread's states:            <BR>
 *  not started, running, dead.                                 <BR>
 * <BR>
 * The test has three phases and works as follows.              <BR>
 * <BR>
 * In first phase,                                              <BR>
 * upon launching debuggee's VM which will be suspended,                <BR>
 * a debugger waits for the VMStartEvent within a predefined            <BR>
 * time interval. If no the VMStartEvent received, the test is FAILED.  <BR>
 * Upon getting the VMStartEvent, it makes the request for debuggee's   <BR>
 * ClassPrepareEvent with SUSPEND_EVENT_THREAD, resumes the VM,         <BR>
 * and waits for the event within the predefined time interval.         <BR>
 * If no the ClassPrepareEvent received, the test is FAILED.            <BR>
 * Upon getting the ClassPrepareEvent,                                  <BR>
 * the debugger sets up the breakpoint with SUSPEND_EVENT_THREAD        <BR>
 * within debuggee's special methodForCommunication().                  <BR>
 * <BR>
 * In second phase to check the assertion,                              <BR>
 * the debugger and the debuggee perform the following.                 <BR>
 * - The debugger resumes the debuggee and waits for the BreakpointEvent.<BR>
 * - The debuggee creates new threads, thread1, and invokes             <BR>
 *   the methodForCommunication to be suspended and                     <BR>
 *   to inform the debugger with the event.                             <BR>
 * - Upon getting the BreakpointEvent, the debugger                     <BR>
 *   - creates a BreakpointRequest1 and                                 <BR>
 *     invokes the method addThreadFilter(tread1) on the request1,      <BR>
 *   - creates a BreakpointRequest2, starts the thread2, and            <BR>
 *     invokes the method addThreadFilter(thread) on the request2,      <BR>
 *   - creates a BreakpointRequest3, waits the thread2 to die, and      <BR>
 *     invokes the method addThreadFilter(thread) on the request3.      <BR>
 * <BR>
 * In third phase, at the end of the test,                              <BR>
 * the debuggee changes the value of the "instruction"  which           <BR>
 * the debugger and debuggee use to inform each other of needed actions,<BR>
 * and both end.
 * <BR>
 */

public class threadfilter003 extends JDIBase {

    //----------------------------------------------------- main method

    public static void main (String argv[]) {

        int result = run(argv, System.out);

        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {

        int exitCode = new threadfilter003().runThis(argv, out);

        if (exitCode != PASSED) {
            System.out.println("TEST FAILED");
        }
        return testExitCode;
    }


    //  ************************************************    test parameters

    private String debuggeeName =
        "nsk.jdi.BreakpointRequest.addThreadFilter.threadfilter003a";

    private String testedClassName =
      "nsk.jdi.BreakpointRequest.addThreadFilter.threadfilter003aTestClass";

    //====================================================== test program

    private int runThis (String argv[], PrintStream out) {

        argsHandler     = new ArgumentHandler(argv);
        logHandler      = new Log(out, argsHandler);
        Binder binder   = new Binder(argsHandler, logHandler);

        waitTime        = argsHandler.getWaitTime() * 60000;

        try {
            log2("launching a debuggee :");
            log2("       " + debuggeeName);
            if (argsHandler.verbose()) {
                debuggee = binder.bindToDebugee(debuggeeName + " -vbs");
            } else {
                debuggee = binder.bindToDebugee(debuggeeName);
            }
            if (debuggee == null) {
                log3("ERROR: no debuggee launched");
                return FAILED;
            }
            log2("debuggee launched");
        } catch ( Exception e ) {
            log3("ERROR: Exception : " + e);
            log2("       test cancelled");
            return FAILED;
        }

        debuggee.redirectOutput(logHandler);

        vm = debuggee.VM();

        eventQueue = vm.eventQueue();
        if (eventQueue == null) {
            log3("ERROR: eventQueue == null : TEST ABORTED");
            vm.exit(PASS_BASE);
            return FAILED;
        }

        log2("invocation of the method runTest()");
        switch (runTest()) {

            case 0 :  log2("test phase has finished normally");
                      log2("   waiting for the debuggee to finish ...");
                      debuggee.waitFor();

                      log2("......getting the debuggee's exit status");
                      int status = debuggee.getStatus();
                      if (status != PASS_BASE) {
                          log3("ERROR: debuggee returned UNEXPECTED exit status: " +
                              status + " != PASS_BASE");
                          testExitCode = FAILED;
                      } else {
                          log2("......debuggee returned expected exit status: " +
                              status + " == PASS_BASE");
                      }
                      break;

            default : log3("ERROR: runTest() returned unexpected value");

            case 1 :  log3("test phase has not finished normally: debuggee is still alive");
                      log2("......forcing: vm.exit();");
                      testExitCode = FAILED;
                      try {
                          vm.exit(PASS_BASE);
                      } catch ( Exception e ) {
                          log3("ERROR: Exception : " + e);
                      }
                      break;

            case 2 :  log3("test cancelled due to VMDisconnectedException");
                      log2("......trying: vm.process().destroy();");
                      testExitCode = FAILED;
                      try {
                          Process vmProcess = vm.process();
                          if (vmProcess != null) {
                              vmProcess.destroy();
                          }
                      } catch ( Exception e ) {
                          log3("ERROR: Exception : " + e);
                      }
                      break;
            }

        return testExitCode;
    }


   /*
    * Return value: 0 - normal end of the test
    *               1 - ubnormal end of the test
    *               2 - VMDisconnectedException while test phase
    */

    private int runTest() {

        try {
            testRun();

            log2("waiting for VMDeathEvent");
            getEventSet();
            if (eventIterator.nextEvent() instanceof VMDeathEvent)
                return 0;

            log3("ERROR: last event is not the VMDeathEvent");
            return 1;
        } catch ( VMDisconnectedException e ) {
            log3("ERROR: VMDisconnectedException : " + e);
            return 2;
        } catch ( Exception e ) {
            log3("ERROR: Exception : " + e);
            return 1;
        }

    }

    private void testRun()
                 throws JDITestRuntimeException, Exception {

        eventRManager = vm.eventRequestManager();

        ClassPrepareRequest cpRequest = eventRManager.createClassPrepareRequest();
        cpRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        cpRequest.addClassFilter(debuggeeName);

        cpRequest.enable();
        vm.resume();
        getEventSet();
        cpRequest.disable();

        ClassPrepareEvent event = (ClassPrepareEvent) eventIterator.next();
        debuggeeClass = event.referenceType();

        if (!debuggeeClass.name().equals(debuggeeName))
            throw new JDITestRuntimeException("** Unexpected ClassName for ClassPrepareEvent **");

        log2("      received: ClassPrepareEvent for debuggeeClass");

        String bPointMethod = "methodForCommunication";
        String lineForComm = "lineForComm";

        ThreadReference mainThread = debuggee.threadByNameOrThrow("main");

        BreakpointRequest bpRequest = settingBreakpoint(mainThread,
                debuggeeClass,
                bPointMethod, lineForComm, "zero");
        bpRequest.enable();

        //------------------------------------------------------  testing section

        log1("     TESTING BEGINS");

        EventRequest eventRequest1 = null;
        EventRequest eventRequest2 = null;
        EventRequest eventRequest3 = null;

        ThreadReference thread1 = null;
        String thread1Name = "thread1";

        String property1 = "BreakpointRequest1";
        String property2 = "BreakpointRequest2";
        String property3 = "BreakpointRequest3";

        String methodName = "method";
        String bpLineName = "breakpointLine";

        ReferenceType testClassReference = null;

        for (int i = 0; ; i++) {

            vm.resume();
            breakpointForCommunication();

            int instruction = ((IntegerValue)
                    (debuggeeClass.getValue(debuggeeClass.fieldByName("instruction")))).value();

            if (instruction == 0) {
                vm.resume();
                break;
            }

            log1(":::::: case: # " + i);

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ variable part

            switch (i) {

                case 0:
                    testClassReference =
                            (ReferenceType) vm.classesByName(testedClassName).get(0);

                    thread1 = (ThreadReference) debuggeeClass.getValue(
                            debuggeeClass.fieldByName(thread1Name));

                    eventRequest1 = setting2BreakpointRequest(null,
                            testClassReference, methodName, bpLineName,
                            EventRequest.SUSPEND_NONE, property1);

                    try {
                        log2("......eventRequest1.addThreadFilter(thread1);");
                        log2("        no Exception expected");
                        ((BreakpointRequest) eventRequest1).addThreadFilter(thread1);
                        log2("        no Exception");
                    } catch (Exception e) {
                        log3("ERROR: Exception : " + e);
                        testExitCode = FAILED;
                    }

                    break;

                case 1:
                    eventRequest2 = setting2BreakpointRequest(null,
                            testClassReference, methodName, bpLineName,
                            EventRequest.SUSPEND_NONE, property2);

                    try {
                        log2("......eventRequest2.addThreadFilter(thread1);");
                        log2("        no Exception expected");
                        ((BreakpointRequest) eventRequest2).addThreadFilter(thread1);
                        log2("        no Exception");
                    } catch (Exception e) {
                        log3("ERROR: Exception : " + e);
                        testExitCode = FAILED;
                    }
                    break;

                case 2:
                    eventRequest3 = setting2BreakpointRequest(null,
                            testClassReference, methodName, bpLineName,
                            EventRequest.SUSPEND_NONE, property3);

                    try {
                        log2("......eventRequest3.addThreadFilter(thread1);");
                        log2("        no Exception expected");
                        ((BreakpointRequest) eventRequest3).addThreadFilter(thread1);
                        log2("        no Exception");
                    } catch (Exception e) {
                        log3("ERROR: Exception : " + e);
                        testExitCode = FAILED;
                    }
                    break;

                default:
                    throw new JDITestRuntimeException("** default case 2 **");
            }
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }
        log1("    TESTING ENDS");
        return;
    }


    // ============================== test's additional methods

    private BreakpointRequest setting2BreakpointRequest ( ThreadReference thread,
                                                          ReferenceType   testedClass,
                                                          String          methodName,
                                                          String          bpLine,
                                                          int             suspendPolicy,
                                                          String          property        )
            throws JDITestRuntimeException {
        try {
            log2("......setting up a breakpoint:");
            log2("       thread: " + thread + "; class: " + testedClass + "; method: " + methodName + "; line: " + bpLine);

            int n = ( (IntegerValue) testedClass.getValue(testedClass.fieldByName(bpLine) ) ).value();
            Location loc = (Location) ((Method) testedClass.methodsByName(methodName).get(0)).allLineLocations().get(n);

            BreakpointRequest
            bpr = eventRManager.createBreakpointRequest(loc);
            bpr.putProperty("number", property);
            if (thread != null)
                bpr.addThreadFilter(thread);
            bpr.setSuspendPolicy(suspendPolicy);

            log2("      a breakpoint has been set up");
            return bpr;
        } catch ( Exception e ) {
            log3("ERROR: ATTENTION: Exception within settingBreakpointRequest() : " + e);
            log3("       BreakpointRequest HAS NOT BEEN SET UP");
            throw new JDITestRuntimeException("** FAILURE to set up BreakpointRequest **");
        }
    }

}
