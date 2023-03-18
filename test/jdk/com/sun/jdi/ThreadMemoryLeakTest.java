/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8297638
 * @summary JDI memory leak when creating and destroying many threads
 *
 * @comment Don't allow -Xcomp or -Xint as they impact memory useage and number of iterations
 * @requires (vm.compMode == "Xmixed")
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g ThreadMemoryLeakTest.java
 * @comment run with -Xmx6m so any leak will quickly produce OOME
 * @run main/othervm -Xmx6m ThreadMemoryLeakTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.LongAdder;

    /********** target program **********/

class ThreadMemoryLeakTarg {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Howdy!");
        Semaphore semaphore = new Semaphore(500);
        LongAdder adder = new LongAdder();
        long startTime = System.currentTimeMillis();
        int iterations = 0;
        // Run for 100 seconds
        while (System.currentTimeMillis() - startTime < 100 * 1000) {
            iterations++;
            semaphore.acquire();
            Executors.defaultThreadFactory().newThread(() -> {
                    adder.increment();
                    long sum = adder.sum();
                    if ((sum % 1000) == 0) {
                        System.out.println("Progress: " + sum);
                    }
                    try {
                        Thread.sleep(50);
                    }
                    catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    } finally {
                        semaphore.release();
                    }
                }).start();
        }
        System.out.println("Goodbye from ThreadMemoryLeakTarg after " + iterations + " iterations!");
    }
}

    /********** test program **********/

public class ThreadMemoryLeakTest extends TestScaffold {
    StepRequest stepRequest = null;
    EventRequestManager erm;
    boolean mainIsDead;

    ThreadMemoryLeakTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new ThreadMemoryLeakTest(args).startTests();
    }

    /********** event handlers **********/

    static int threadStartCount;
    static int threadDeathCount;
    private static List<ThreadReference> threads =
        Collections.synchronizedList(new ArrayList<ThreadReference>());

    public void threadStarted(ThreadStartEvent event) {
        threadStartCount++;
        if ((threadStartCount % 1000) == 0) {
            println("Got ThreadStartEvent #" + threadStartCount +
                               " threads:" + threads.size());
        }
        ThreadStartEvent tse = (ThreadStartEvent)event;
        threads.add(tse.thread());
    }

    public void threadDied(ThreadDeathEvent event) {
        threadDeathCount++;
        if ((threadDeathCount % 1000) == 0) {
            println("Got ThreadDeathEvent #" + threadDeathCount +
                               " threads:" + threads.size());
        }
        ThreadDeathEvent tde = (ThreadDeathEvent)event;
        ThreadReference thread = tde.thread();
        threads.remove(thread);
    }

    public void vmDied(VMDeathEvent event) {
        println("Got VMDeathEvent");
    }

    public void vmDisconnected(VMDisconnectEvent event) {
        println("Got VMDisconnectEvent");
    }

    /********** test core **********/

    protected void runTests() throws Exception {
        /*
         * Launch debuggee and break at main() method.
         */
        BreakpointEvent bpe = startToMain("ThreadMemoryLeakTarg");

        /*
         * Setup ThreadStart and ThreadDeath event requests. Note, SUSPEND_NONE is important
         * for this test. Otherwise the memory leak described in 8297638 is not triggered.
         * There can't be any events coming in that might result in a suspend since the
         * resume will clear out the leak.
         */
        erm = vm().eventRequestManager();
        ThreadStartRequest tsrReq = erm.createThreadStartRequest();
        ThreadDeathRequest tdrReq = erm.createThreadDeathRequest();
        tsrReq.setSuspendPolicy(EventRequest.SUSPEND_NONE);
        tdrReq.setSuspendPolicy(EventRequest.SUSPEND_NONE);
        tsrReq.enable();
        tdrReq.enable();

        /*
         * Resume the target and listen for events
         */
        listenUntilVMDisconnect();

        /*
         * Any test failure will result in an exception or a timeout. So if we
         * get here we passed.
         */
        println("ThreadMemoryLeakTest: PASSED");
    }
}
