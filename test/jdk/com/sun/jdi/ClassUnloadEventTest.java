/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8256811
 * @modules java.base/jdk.internal.org.objectweb.asm
 *          java.base/jdk.internal.misc
 * @library /test/lib
 * @build jdk.test.whitebox.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @run main/othervm/native ClassUnloadEventTest run
 */

import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.test.lib.classloader.ClassUnloadCommon;

import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;
import java.io.*;

public class ClassUnloadEventTest {
    static final String CLASS_NAME_PREFIX = "SampleClass__";
    static final String CLASS_NAME_ALT_PREFIX = CLASS_NAME_PREFIX + "Alt__";
    static final int NUM_CLASSES = 10;
    static final int NUM_ALT_CLASSES = NUM_CLASSES / 2;

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            try {
                runDebuggee();
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        } else {
            runDebugger();
        }
    }

    private static class TestClassLoader extends ClassLoader implements Opcodes {
        private static byte[] generateSampleClass(String name) {
            ClassWriter cw = new ClassWriter(0);

            cw.visit(52, ACC_SUPER | ACC_PUBLIC, name, null, "java/lang/Object", null);
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "m", "()V", null, null);
            mv.visitCode();
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
            cw.visitEnd();
            return cw.toByteArray();
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (name.startsWith(CLASS_NAME_PREFIX)) {
                byte[] bytecode = generateSampleClass(name);
                return defineClass(name, bytecode, 0, bytecode.length);
            } else {
                return super.findClass(name);
            }
        }
    }

    private static void runDebuggee() {
        System.out.println("Running debuggee");
        ClassLoader loader = new TestClassLoader();
        for (int index = 0; index < NUM_CLASSES; index++) {
            try {
                if (index < NUM_ALT_CLASSES) {
                    Class.forName(CLASS_NAME_ALT_PREFIX + index, true, loader);
                } else {
                    Class.forName(CLASS_NAME_PREFIX + index, true, loader);
                }
            } catch (Exception e) {
<<<<<<< HEAD
                throw new RuntimeException("Failed to create Sample class", e);
=======
                throw new RuntimeException("Failed to create Sample class");
>>>>>>> 5977ce5adf85c2fb9b1dd62ed250f6d4f05a613c
            }
        }
        loader = null;
        // Trigger class unloading
        ClassUnloadCommon.triggerUnloading();

        // Short delay to make sure all ClassUnloadEvents have been sent
        // before VMDeathEvent is genareated.
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
        }
<<<<<<< HEAD

        System.out.println("Exiting debuggee");
=======
>>>>>>> 5977ce5adf85c2fb9b1dd62ed250f6d4f05a613c
    }

    private static void runDebugger() throws Exception {
        System.out.println("Running debugger");
        HashSet<String> unloadedSampleClasses = new HashSet<>();
        HashSet<String> unloadedSampleClasses_alt = new HashSet<>();
        VirtualMachine vm = null;
        vm = connectAndLaunchVM();
        ClassUnloadRequest classUnloadRequest = vm.eventRequestManager().createClassUnloadRequest();
        classUnloadRequest.addClassFilter(CLASS_NAME_PREFIX + "*");
        classUnloadRequest.enable();

        ClassUnloadRequest classUnloadRequest_alt = vm.eventRequestManager().createClassUnloadRequest();
        classUnloadRequest_alt.addClassFilter(CLASS_NAME_ALT_PREFIX + "*");
        classUnloadRequest_alt.enable();

        EventSet eventSet = null;
        boolean exited = false;
        while (!exited && (eventSet = vm.eventQueue().remove()) != null) {
            System.out.println("EventSet: " + eventSet);
            for (Event event : eventSet) {
                if (event instanceof ClassUnloadEvent) {
                    String className = ((ClassUnloadEvent)event).className();

                    // The unloaded class should always match CLASS_NAME_PREFIX.
                    if (className.indexOf(CLASS_NAME_PREFIX) == -1) {
                        throw new RuntimeException("FAILED: Unexpected unloaded class: " + className);
                    }

                    // Unloaded classes with ALT names should only occur on the classUnloadRequest_alt.
                    if (event.request() == classUnloadRequest_alt) {
                        unloadedSampleClasses_alt.add(className);
                        if (className.indexOf(CLASS_NAME_ALT_PREFIX) == -1) {
                            throw new RuntimeException("FAILED: non-alt class unload event for classUnloadRequest_alt.");
                        }
                    } else {
                        unloadedSampleClasses.add(className);
                    }

                    // If the unloaded class matches the ALT prefix, then we should have
                    // unload events in this EventSet for each of the two ClassUnloadRequesta.
                    int expectedEventSetSize;
                    if (className.indexOf(CLASS_NAME_ALT_PREFIX) != -1) {
                        expectedEventSetSize = 2;
                    } else {
                        expectedEventSetSize = 1;
                    }
                    if (eventSet.size() != expectedEventSetSize) {
                        throw new RuntimeException("FAILED: Unexpected eventSet size: " + eventSet.size());
                    }
                }

                if (event instanceof VMDeathEvent) {
                    exited = true;
                    break;
                }
            }
            eventSet.resume();
        }

<<<<<<< HEAD
        /* Dump debuggee output. */
        Process p = vm.process();
        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        String line = in.readLine();
        while (line != null) {
            System.out.println("stdout: " + line);
            line = in.readLine();
        }
        line = err.readLine();
        while (line != null) {
            System.out.println("stderr: " + line);
            line = err.readLine();
        }

=======
>>>>>>> 5977ce5adf85c2fb9b1dd62ed250f6d4f05a613c
        if (unloadedSampleClasses.size() != NUM_CLASSES) {
            throw new RuntimeException("Wrong number of class unload events: expected " + NUM_CLASSES + " got " + unloadedSampleClasses.size());
        }
        if (unloadedSampleClasses_alt.size() != NUM_ALT_CLASSES) {
            throw new RuntimeException("Wrong number of alt class unload events: expected " + NUM_ALT_CLASSES + " got " + unloadedSampleClasses_alt.size());
        }
    }

    private static VirtualMachine connectAndLaunchVM() throws IOException,
                                                              IllegalConnectorArgumentsException,
                                                              VMStartException {
        LaunchingConnector launchingConnector = Bootstrap.virtualMachineManager().defaultConnector();
        Map<String, Connector.Argument> arguments = launchingConnector.defaultArguments();
        arguments.get("main").setValue(ClassUnloadEventTest.class.getName());
<<<<<<< HEAD
        arguments.get("options").setValue("--add-exports java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xlog:class+unload=info -Xlog:gc");
=======
        arguments.get("options").setValue("--add-exports java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI");
>>>>>>> 5977ce5adf85c2fb9b1dd62ed250f6d4f05a613c
        return launchingConnector.launch(arguments);
    }
}
