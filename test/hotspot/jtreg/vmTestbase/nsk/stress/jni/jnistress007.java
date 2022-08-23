/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * jnistress007 is a class that sets up classes that do the work
 * for the test.
 *
 * The Interrupter objects send interrupts to the JNIters.
 * The GarbageGenerator objects generate garbage.
 *
 * sync[0] synchronizes the test cycles.
 * sync[1] synchronizes access to exception counters.
 * sync[2] synchronizes the cycle count update.  It also insures that
 *         the interrupts do not interfere with the cycle count updates.
 *         This is because cycle count updates are used to define cycles.
 */


/*
 * @test
 * @key stress
 *
 * @summary converted from VM testbase nsk/stress/jni/jnistress007.
 * VM testbase keywords: [stress, quick, feature_283, nonconcurrent]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native
 *      nsk.stress.jni.jnistress007
 *      -numTHREADer 10
 *      -threadInterval 20
 *      -numInterrupter 2
 *      -interruptInterval 50
 *      -numGarbage 80
 *      -garbageInterval 5
 *      -numIteration 130
 */

package nsk.stress.jni;

import nsk.share.Consts;
import nsk.share.Debug;
import nsk.share.test.StressOptions;

public class jnistress007 extends Thread {

    /* Maximum number of iterations.  Ignored if <= 0L */
    static long numIteration = 0L;
    /* Timeout */
    static long timeOut;
    /* Number of test class objects */
    static int numJNIter = 1;
    /* Time between JNI stressing by the threads under test */
    /* (in milliseconds) */
    static int jniInterval = 10000;
    /* Number of interrupting threads */
    static int numInterrupter = 1;
    /* Time between interrupts in milliseconds */
    static int interruptInterval = 1000;
    /* Number of garbage generating threads */
    static int numGarbage = 1;
    /* Time between garbage allocations in milliseconds */
    static int garbageInterval = 1000;
    // The MAX quantity of monitor's call
    static int jniStringAllocSize = 300;

    private static StressOptions stressOptions;

    public static void main(String[] argv) {
        try {
            int i = 0;
            int nJNISync = 10;
            jnistress007 dm = null;
            boolean errArg = false;

            stressOptions = new StressOptions(argv);

                        /* Process arguments */
            while (!errArg && i < argv.length) {
                            /* Number of iterations. Ignored if <= 0. */
                if (i < argv.length && argv[i].equals("-numIteration")) {
                    ++i;
                    if (i < argv.length && Character.isDigit(argv[i].charAt(0))) {
                        try {
                            numIteration = Long.parseLong(argv[i++]);
                        } catch (NumberFormatException e) {
                            errArg = true;
                        }
                    }
                } else if (i < argv.length && argv[i].equals("-numTHREADer")) {
                    ++i;
                    if (i < argv.length && Character.isDigit(argv[i].charAt(0))) {
                        try {
                            numJNIter = Integer.parseInt(argv[i++]);
                        } catch (NumberFormatException e) {
                            errArg = true;
                        }
                        if (numJNIter <= 0) errArg = true;
                    }
                } else if (i < argv.length && argv[i].equals("-threadInterval")) {
                    ++i;
                    if (i < argv.length && Character.isDigit(argv[i].charAt(0))) {
                        try {
                            jniInterval = Integer.parseInt(argv[i++]);
                        } catch (NumberFormatException e) {
                            errArg = true;
                        }
                    }
                } else if (i < argv.length && argv[i].equals("-numInterrupter")) {
                    ++i;
                    if (i < argv.length && Character.isDigit(argv[i].charAt(0))) {
                        try {
                            numInterrupter = Integer.parseInt(argv[i++]);
                        } catch (NumberFormatException e) {
                            errArg = true;
                        }
                    }
                } else if (i < argv.length && argv[i].equals("-interruptInterval")) {
                    ++i;
                    if (i < argv.length && Character.isDigit(argv[i].charAt(0))) {
                        try {
                            interruptInterval = Integer.parseInt(argv[i++]);
                        } catch (NumberFormatException e) {
                            errArg = true;
                        }
                    }
                } else if (i < argv.length && argv[i].equals("-numGarbage")) {
                    ++i;
                    if (i < argv.length && Character.isDigit(argv[i].charAt(0))) {
                        try {
                            numGarbage = Integer.parseInt(argv[i++]);
                        } catch (NumberFormatException e) {
                            errArg = true;
                        }
                    }
                } else if (i < argv.length && argv[i].equals("-garbageInterval")) {
                    ++i;
                    if (i < argv.length && Character.isDigit(argv[i].charAt(0))) {
                        try {
                            garbageInterval = Integer.parseInt(argv[i++]);
                        } catch (NumberFormatException e) {
                            errArg = true;
                        }
                    }
                } else if (i < argv.length && argv[i].equals("-jniStringAllocSize")) {
                    ++i;
                    if (i < argv.length && Character.isDigit(argv[i].charAt(0))) {
                        try {
                            jniStringAllocSize = Integer.parseInt(argv[i++]);
                        } catch (NumberFormatException e) {
                            errArg = true;
                        }
                    }
                } else if (i < argv.length && argv[i].startsWith("-stress")) {
                    ++i;
                    if (i < argv.length && Character.isDigit(argv[i].charAt(0))) {
                        ++i;
                    }
                } else System.out.println("Argument #" + i++ + " is incorrect");
            }

            numIteration *= stressOptions.getIterationsFactor();
            numJNIter *= stressOptions.getThreadsFactor();
            numInterrupter *= stressOptions.getThreadsFactor();
            numGarbage *= stressOptions.getThreadsFactor();
            timeOut = stressOptions.getTime() * 1000;

            sync = new Synchronizer[10];
            for (i = 0; i < nJNISync; i++)
                sync[i] = new Synchronizer();
            dm = new jnistress007(numIteration, numJNIter, jniInterval,
                    numInterrupter, interruptInterval, numGarbage, garbageInterval);
            dm.start();

            try {
                dm.join(timeOut);
            } catch (InterruptedException e) {
                System.out.println("TESTER THREAD WAS INTERRUPTED");
                System.exit(Consts.TEST_FAILED);
            }

            if (DEBUG) System.out.println("jnistress007::main(): halt!");

            if (dm.isAlive()) {
                System.out.println("TIME LIMIT EXCEEDED");
                dm.halt();
                if (DEBUG) System.out.println("jnistress007::main(): join!");
                try {
                    dm.join(10000L);
                } catch (InterruptedException e) {
                    System.out.println("TESTER THREAD WAS INTERRUPTED");
                    System.exit(Consts.TEST_FAILED);
                }
            } else {
                System.out.println("TESTER THREAD FINISHED");
            }

            if (DEBUG) System.out.println("jnistress007::main(): zzzz...");

            if (JNIter007.passed())
                System.exit(Consts.TEST_FAILED);

        } catch (Throwable e) {
            Debug.Fail(e);
        }
    }

    jnistress007(
            long iters,
            int nJNI,
            int jniInterval,
            int nInter,
            int iruptInterval,
            int nGarb,
            int garbInterval
    ) {
        int i = 0;
        nCycles = iters;
        /* Should have at least one of nCycles>0 */
        if (nCycles <= 0) nCycles = Long.MAX_VALUE;
        jniter = new JNIter007[nJNI];
        interval = jniInterval;
        irupt = new Interrupter[nInter];
        garb = new GarbageGenerator[nGarb];
        for (i = 0; i < nJNI; i++)
            jniter[i] = new JNIter007(sync);
        for (i = 0; i < nInter; i++) {
            irupt[i] = new Interrupter(jniter, sync);
            irupt[i].setInterval(iruptInterval);
        }
        for (i = 0; i < nGarb; i++) {
            garb[i] = new GarbageGenerator();
            garb[i].setInterval(garbInterval);
        }
    }

    public void run() {
        try {
            int i = 0;
            long iCycle = 0L;
            JNIter007.clearCount();
            JNIter007.clearInterruptCount();
            for (i = 0; i < jniter.length; i++)
                jniter[i].start();

            while (JNIter007.getCount() < jniter.length) {
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                }
            }
            JNIter007.clearCount();
            // JNIter007.clearInterruptCount();
            synchronized (sync[0]) {
                sync[0].notifyAll();
            }

            for (i = 0; i < garb.length; i++)
                garb[i].start();
            for (i = 0; i < irupt.length; i++)
                irupt[i].start();

            if (DEBUG) System.out.println("Cycles=" + nCycles);
            for (iCycle = 0; iCycle < nCycles && !done && !JNIter007.passed(); iCycle++) {
                System.out.print("Cycle: " + iCycle);
                try {
                    sleep(interval);
                } catch (InterruptedException e) {
                }
                synchronized (sync[1]) {
                    System.out.println("\tInterrupt count=" +
                            JNIter007.getInterruptCount());
                }
                JNIter007.clearCount();
                synchronized (sync[0]) {
                    sync[0].notifyAll();
                }
                int n = 0;
                for (i = 0; i < jniter.length; i++)
                    if (jniter[i].finished()) n++;
                if (n == jniter.length) break;
            }
            for (i = 0; i < irupt.length; i++)
                irupt[i].halt();
            for (i = 0; i < garb.length; i++)
                garb[i].halt();
            for (i = 0; i < jniter.length; i++)
                jniter[i].halt();
                        /* Flush any waiters */
            if (DEBUG) System.out.println("jnistress007::run(): before sync[0]");
            synchronized (sync[0]) {
                sync[0].notifyAll();
            }
            if (DEBUG) System.out.println("jnistress007::run(): after sync[0]");
            for (i = 0; i < irupt.length; i++) {
                try {
                    irupt[i].join();
                } catch (InterruptedException e) {
                }
            }
            if (DEBUG) System.out.println("jnistress007::run(): X");
            for (i = 0; i < garb.length; i++) {
                try {
                    garb[i].join();
                } catch (InterruptedException e) {
                }
            }
            if (DEBUG) System.out.println("jnistress007::run(): Y");
            synchronized (sync[0]) {
                sync[0].notifyAll();
            }
            for (i = 0; i < jniter.length; i++) {
                try {
                    jniter[i].join();
                } catch (InterruptedException e) {
                }
            }
            if (JNIter007.passed()) {
                System.out.println("JNI TEST PASSED");
            } else {
                System.out.println("JNI TEST FAILED");
            }
            if (DEBUG) System.out.println("jnistress007::run(): Z");
        } catch (Throwable e) {
            Debug.Fail(e);
        }
    }

    public void halt() {
        done = true;
    }

    public boolean finished() {
        return done;
    }

    long nCycles = 0;
    JNIter007[] jniter;
    static Synchronizer[] sync;
    private int interval = 100;
    Interrupter[] irupt;
    GarbageGenerator[] garb;
    private boolean done = false;
    final private static boolean DEBUG = false;
}

class JNIter007 extends Thread {

    // The native methods for testing JNI monitors calls
    public native void incCount(String name);

    static {
        System.loadLibrary("jnistress007");
    }

    public static int nativeCount, javaCount = 0;

    public JNIter007(Synchronizer[] aSync) {
        sync = aSync;
    }

    public void run() {
        try {
            int iter = 0;

                        /* Synchronize start of work */
            incCount();
            synchronized (sync[0]) {
                try {
                    sync[0].wait();
                } catch (InterruptedException e) {
                }
            }
            while (!done && !pass) {
                try {
                                /* Synchronized the JNI stressing */
                    synchronized (sync[2]) {
                        incCount();
                    }
                    synchronized (sync[0]) {
                        try {
                            sync[0].wait();
                        } catch (InterruptedException e) {
                            synchronized (sync[1]) {
                                JNIter007.incInterruptCount();
                            }
                        }
                    }
                    synchronized (sync[0]) {
                        try {
                            if ((javaCount < jnistress007.jniStringAllocSize) &&
                                    (nativeCount < jnistress007.jniStringAllocSize)) {
                                javaCount++;
                                incCount(getName());
                                if ((javaCount % 1000) == 0)
                                    System.out.println("Count in java " +
                                            getName() + " " + javaCount);
                            } else if (javaCount == nativeCount) {
                                done = true;
                            } else {
                                System.out.println("Native: " + nativeCount +
                                        "\t" + "Java: " + javaCount);
                                pass = true;
                            }
                        } catch (Exception e) {
                            System.out.println("Error: " + e);
                        }
                    }
                    if (DEBUG)
                        System.out.println("We have " + activeCount() + " threads now.");
                    synchronized (this) {
                        try {
                            wait(1L);
                        } catch (InterruptedException e) {
                            throw new InterruptedException();
                        }
                    }
                } catch (Exception e) {
                    synchronized (sync[1]) {
                        JNIter007.incInterruptCount();
                    }
                }
                iter++;
                iter = iter % CASECOUNT;
            }
            if (DEBUG) System.out.println("JNITer::run(): done=" + done);
            done = true;
            if (DEBUG) System.out.println("JNITer::run(): pass=" + JNIter007.pass);
            if (DEBUG) System.out.println("JNIter::run(): done");
        } catch (Throwable e) {
            Debug.Fail(e);
        }
    }

    private synchronized static void incCount() {
        count++;
    }

    public static int getCount() {
        return count;
    }

    public synchronized static void clearCount() {
        count = 0;
    }

    private synchronized static void incInterruptCount() {
        interruptCount++;
    }

    public static int getInterruptCount() {
        return interruptCount;
    }

    public synchronized static void clearInterruptCount() {
        interruptCount = 0;
    }

    public void halt() {
        done = true;
    }

    public boolean finished() {
        return done;
    }

    public static boolean passed() {
        return pass;
    }

    Synchronizer[] sync;
    private static int count = 0;
    private static int interruptCount = 0;
    private boolean done = false;
    private static boolean pass = false;
    final private static int CASECOUNT = 2;
    final private static boolean DEBUG = false;
}
