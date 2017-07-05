/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

/*
 * @test
 * @bug 8136421
 * @requires (vm.simpleArch == "x64" | vm.simpleArch == "sparcv9" | vm.simpleArch == "aarch64")
 * @library /testlibrary /test/lib /
 * @library ../common/patches
 * @modules java.base/jdk.internal.misc
 * @modules java.base/jdk.internal.org.objectweb.asm
            java.base/jdk.internal.org.objectweb.asm.tree
            jdk.vm.ci/jdk.vm.ci.hotspot
 *          jdk.vm.ci/jdk.vm.ci.meta
 *          jdk.vm.ci/jdk.vm.ci.code
 *
 * @build jdk.vm.ci/jdk.vm.ci.hotspot.CompilerToVMHelper
 * @build compiler.jvmci.compilerToVM.GetConstantPoolTest
 * @run main/othervm -Xbootclasspath/a:.
 *                   -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI
 *                   compiler.jvmci.compilerToVM.GetConstantPoolTest
 */
package compiler.jvmci.compilerToVM;

import jdk.test.lib.Utils;
import compiler.jvmci.common.CTVMUtilities;
import compiler.jvmci.common.testcases.TestCase;
import jdk.vm.ci.hotspot.CompilerToVMHelper;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaMethod;
import jdk.vm.ci.hotspot.HotSpotResolvedObjectType;
import jdk.vm.ci.meta.ConstantPool;

import java.lang.reflect.Field;
import java.lang.reflect.Executable;

/**
 * Tests for jdk.vm.ci.hotspot.CompilerToVM::getConstantPool method
 */
public class GetConstantPoolTest {

    public static void testMethod(Executable executable) {
        test(CTVMUtilities.getResolvedMethod(executable));
    }

    public static void testClass(Class cls) {
        HotSpotResolvedObjectType type = CompilerToVMHelper
                .lookupType(Utils.toJVMTypeSignature(cls),
                        GetConstantPoolTest.class, /* resolve = */ true);
        test(type);
    }

    private static void test(Object object) {
        ConstantPool cp = CompilerToVMHelper.getConstantPool(object);
        System.out.println(object + " -> " + cp);
    }

    public static void main(String[] args) {
        TestCase.getAllClasses().forEach(GetConstantPoolTest::testClass);
        TestCase.getAllExecutables().forEach(GetConstantPoolTest::testMethod);
        testNull();
        testObject();
    }

    private static void testNull() {
        try {
            Object cp = CompilerToVMHelper.getConstantPool(null);
            throw new AssertionError("Test OBJECT."
                + " Expected IllegalArgumentException has not been caught");
        } catch (NullPointerException npe) {
            // expected
        }
    }
    private static void testObject() {
        try {
            Object cp = CompilerToVMHelper.getConstantPool(new Object());
            throw new AssertionError("Test OBJECT."
                + " Expected IllegalArgumentException has not been caught");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }
}
