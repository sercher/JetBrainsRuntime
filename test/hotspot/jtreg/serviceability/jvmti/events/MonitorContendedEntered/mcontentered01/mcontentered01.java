/*
 * Copyright (c) 2003, 2023, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.jvmti.DebugeeClass;

/*
 * @test
 *
 * @summary converted from VM Testbase nsk/jvmti/MonitorContendedEntered/mcontentered001.
 * VM Testbase keywords: [quick, jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test exercises JVMTI event callback function
 *         MonitorContendedEntered(jni, thread, object).
 *     The test checks if the thread and object parameters of the function
 *     contain expected values for callback when a thread enters a Java language
 *     monitor after waiting for it to be released by another thread.
 * COMMENTS
 *     The test updated to match new JVMTI spec 0.2.90:
 *     - change signature of agentProc function
 *       and save JNIEnv pointer now passed as argument.
 *
 * @library /test/lib
 * @compile mcontentered01.java
 * @run main/othervm/native -agentlib:mcontentered01 mcontentered01 platform
 * @run main/othervm/native -agentlib:mcontentered01 mcontentered01 virtual
 */

public class mcontentered01 extends DebugeeClass {

    // load native library if required
    static {
        loadLibrary("mcontentered01");
    }

    // run test from command line
    public static void main(String args[]) {
        boolean isVirtual = "virtual".equals(args[0]);
        int result = new mcontentered01().runIt(isVirtual);
        if (result != 0) {
            throw new RuntimeException("Unexpected status: " + result);
        }
    }

    // run debuggee
    public int runIt(boolean isVirtual) {
        int status = TEST_PASSED;
        long timeout =  60000; // milliseconds
        System.out.println("Timeout = " + timeout + " msc.");

        mcontentered01Task task = new mcontentered01Task();
        Thread.Builder builder;
        if (isVirtual) {
            builder = Thread.ofVirtual();
        } else {
            builder = Thread.ofPlatform();
        }
        Thread thread = builder.name("Debuggee Thread").unstarted(task);
        setExpected(task.endingMonitor, thread);

        synchronized (task.endingMonitor) {

            // run thread
            try {
                // start thread
                synchronized (task.startingMonitor) {
                    thread.start();
                    task.startingMonitor.wait(timeout);
                }
            } catch (InterruptedException e) {
                throw new Failure(e);
            }

            int totalDelay = 0;
            while (getEventCount() < 1 && totalDelay < timeout) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new Failure(e);
                }
                totalDelay += 100;
            }

            Thread.yield();
            System.out.println("Thread started");
        }

        // wait for thread finish
        try {
            thread.join(timeout);
        } catch (InterruptedException e) {
            throw new Failure(e);
        }

        System.out.println("Sync: thread finished");
        status = checkStatus(status);

        return status;
    }

    private native int getEventCount();
    private native void setExpected(Object monitor, Object thread);
}

/* =================================================================== */

class mcontentered01Task implements Runnable {
    public Object startingMonitor = new Object();
    public Object endingMonitor = new Object();

    public void run() {

        mcontentered01.checkStatus(DebugeeClass.TEST_PASSED);

        // notify about starting
        synchronized (startingMonitor) {
            startingMonitor.notify();
        }

        // wait until main thread release monitor
        synchronized (endingMonitor) {
            endingMonitor.notify();
        }
    }
}
