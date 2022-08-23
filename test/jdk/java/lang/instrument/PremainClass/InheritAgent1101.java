/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6289149 8165276
 * @summary test config (1,1,0,1): 2-arg and 1-arg in superclass, and declared 1-arg in agent class
 * @author Daniel D. Daugherty, Sun Microsystems
 *
 * @library /test/lib
 * @build jdk.java.lang.instrument.PremainClass.InheritAgent1101
 * @run driver jdk.test.lib.util.JavaAgentBuilder
 *             InheritAgent1101 InheritAgent1101.jar
 * @run main/othervm -javaagent:InheritAgent1101.jar DummyMain
 */

import java.lang.instrument.*;

public class InheritAgent1101 extends InheritAgent1101Super {

    //
    // This agent has a single argument premain() method which
    // is the one that should be called.
    //
    public static void premain (String agentArgs) {
        System.out.println("Hello from Single-Arg InheritAgent1101!");
    }

    // This agent does NOT have a double argument premain() method.
}

class InheritAgent1101Super {

    //
    // This agent has a single argument premain() method which
    // is NOT the one that should be called.
    //
    public static void premain (String agentArgs) {
        System.out.println("Hello from Single-Arg InheritAgent1101Super!");
        throw new Error("ERROR: THIS AGENT SHOULD NOT HAVE BEEN CALLED.");
    }

    //
    // This agent has a double argument premain() method which
    // is NOT the one that should be called.
    //
    public static void premain (String agentArgs, Instrumentation instArg) {
        System.out.println("Hello from Double-Arg InheritAgent1101Super!");
        throw new Error("ERROR: THIS AGENT SHOULD NOT HAVE BEEN CALLED.");
    }
}
