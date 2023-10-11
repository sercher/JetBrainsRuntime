/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.ClassPrepareRequest.addClassFilter_rt;

import nsk.share.*;
import nsk.share.jdi.*;

/**
 * This class is used as debuggee application for the filter_rt003 JDI test.
 */

public class filter_rt003a {

    //----------------------------------------------------- templete section

    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    static ArgumentHandler argHandler;
    static Log log;

    //--------------------------------------------------   log procedures

    public static void log1(String message) {
        log.display("**> debuggee: " + message);
    }

    private static void logErr(String message) {
        log.complain("**> debuggee: " + message);
    }

    //====================================================== test program
    //------------------------------------------------------ common section

    static int exitCode = PASSED;

    static int instruction = 1;
    static int end         = 0;
                                   //    static int quit        = 0;
                                   //    static int continue    = 2;
    static int maxInstr    = 1;    // 2;

    static int lineForComm = 2;

    private static void methodForCommunication() {
        int i1 = instruction;
        int i2 = i1;
        int i3 = i2;
    }
    //----------------------------------------------------   main method

    public static void main (String argv[]) {

        argHandler = new ArgumentHandler(argv);
        log = argHandler.createDebugeeLog();

        log1("debuggee started!");

        label0:
            for (int i = 0; ; i++) {

                if (instruction > maxInstr) {
                    logErr("ERROR: unexpected instruction: " + instruction);
                    exitCode = FAILED;
                    break ;
                }

                switch (i) {

    //------------------------------------------------------  section tested

                    case 0:
                            filter_rt003aTestClass10 obj1 = new filter_rt003aTestClass10();
                            filter_rt003aTestClass20 obj2 = new filter_rt003aTestClass20();
                            break;


                    case 1:
                            Thread thread1 = JDIThreadFactory.newThread(new filter_rt003aThread1("thread1"));
                            Thread thread2 = JDIThreadFactory.newThread(new filter_rt003aThread2("thread2"));

                            log1("new filter_rt003a().run1(thread1);");
                            new filter_rt003a().run1(thread1);

                            log1("new filter_rt003a().run1(thread2);");
                            new filter_rt003a().run1(thread2);

    //-------------------------------------------------    standard end section

                    default:
                            instruction = end;
                            break;
                }
                log1("methodForCommunication();");
                methodForCommunication();
                if (instruction == end)
                    break;
            }

        log1("debuggee exits");
        System.exit(exitCode + PASS_BASE);
    }

    static Object waitnotifyObj = new Object();

    static int threadStart(Thread t) {
        synchronized (waitnotifyObj) {
            t.start();
            try {
                waitnotifyObj.wait();
            } catch ( Exception e) {
                exitCode = FAILED;
                logErr("       Exception : " + e );
                return FAILED;
            }
        }
        return PASSED;
    }

    public void run1(Thread t) {
        t.start();
        try {
            t.join();
        } catch ( InterruptedException e ) {
        }
    }

}

class filter_rt003aTestClass10{
    static int var10 = 0;
}
class filter_rt003aTestClass11 extends filter_rt003aTestClass10{
    static int var11 = 0;
}
class filter_rt003aThread1 extends NamedTask {

    public filter_rt003aThread1(String threadName) {
        super(threadName);
    }

    public void run() {
        filter_rt003a.log1("  'run': enter  :: threadName == " + getName());
        filter_rt003aTestClass11 obj1 = new filter_rt003aTestClass11();
        filter_rt003a.log1("  'run': exit   :: threadName == " + getName());
        return;
    }
}

class filter_rt003aTestClass20 {
    static int var20 = 0;
}
class filter_rt003aTestClass21 {
    static int var21 = 0;
}
class filter_rt003aThread2 extends NamedTask {

    public filter_rt003aThread2(String threadName) {
        super(threadName);
    }

    public void run() {
        filter_rt003a.log1("  'run': enter  :: threadName == " + getName());
        filter_rt003aTestClass21 obj2 = new filter_rt003aTestClass21();
        filter_rt003a.log1("  'run': exit   :: threadName == " + getName());
        return;
    }
}
