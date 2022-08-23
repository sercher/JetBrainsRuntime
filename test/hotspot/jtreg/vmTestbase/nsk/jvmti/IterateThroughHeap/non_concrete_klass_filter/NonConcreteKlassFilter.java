/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
package nsk.jvmti.IterateThroughHeap.non_concrete_klass_filter;

import java.io.PrintStream;

import nsk.share.*;
import nsk.share.jvmti.*;

public class NonConcreteKlassFilter extends DebugeeClass {

    static {
        loadLibrary("NonConcreteKlassFilter");
    }

    public static void main(String args[]) {
        String[] argv = JVMTITest.commonInit(args);
        System.exit(new NonConcreteKlassFilter().runTest(argv,System.out) + Consts.JCK_STATUS_BASE);
    }

    protected Log log = null;
    protected ArgumentHandler argHandler = null;
    protected int status = Consts.TEST_PASSED;

    static Object objects[] = new Object[] { new InterfaceImplementation(),
                                             new AbstractClassImplementation() };

    public int runTest(String args[], PrintStream out) {
        argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        log.display("Verifying JVMTI_ABORT.");
        status = checkStatus(status);
        return status;
    }

}

interface Interface {
    static long field1 = 0xF1E1D01L;
}

abstract class AbstractClass {
    static long field1 = 0xF1E1D02L;
    long field2 = 0xDEADF1E1D01L;
}

class InterfaceImplementation implements Interface {

}

class AbstractClassImplementation extends AbstractClass {

}
