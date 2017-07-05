/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Module;
import static jdk.test.lib.Asserts.*;

/*
 * @test
 * @modules java.base/jdk.internal.misc
 * @library /testlibrary /test/lib /compiler/whitebox ..
 * @compile p2/c2.java
 * @compile p1/c1.java
 * @build sun.hotspot.WhiteBox
 * @compile/module=java.base java/lang/reflect/ModuleHelper.java
 * @run main ClassFileInstaller sun.hotspot.WhiteBox
 *                              sun.hotspot.WhiteBox$WhiteBoxPermission
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI JVMAddModuleExportsToAll
 */

public class JVMAddModuleExportsToAll {

    // Test a series of error conditions for API JVM_AddModuleExportsToAll()
    // and then test that a class in the unnamed module can access a package in
    // a named module that has been exported unqualifiedly.
    public static void main(String args[]) throws Throwable {
        Object m1, m2, m3;

        // Get the java.lang.reflect.Module object for module java.base.
        Class jlObject = Class.forName("java.lang.Object");
        Object jlObject_jlrM = jlObject.getModule();
        assertNotNull(jlObject_jlrM, "jlrModule object of java.lang.Object should not be null");

        // Get the class loader for JVMAddModuleExportsToAll and assume it's also used to
        // load class p2.c2.
        ClassLoader this_cldr = JVMAddModuleExportsToAll.class.getClassLoader();

        // Define a module for p3.
        m1 = ModuleHelper.ModuleObject("module1", this_cldr, new String[] { "p3" });
        assertNotNull(m1, "Module should not be null");
        ModuleHelper.DefineModule(m1, "9.0", "m1/there", new String[] { "p3" });
        ModuleHelper.AddReadsModule(m1, jlObject_jlrM);

        // Define a module for p2.
        m2 = ModuleHelper.ModuleObject("module2", this_cldr, new String[] { "p2" });
        assertNotNull(m2, "Module should not be null");
        ModuleHelper.DefineModule(m2, "9.0", "m2/there", new String[] { "p2" });
        ModuleHelper.AddReadsModule(m2, jlObject_jlrM);

        try {
            ModuleHelper.AddModuleExportsToAll((Module)null, "p2");
            throw new RuntimeException("Failed to get the expected NPE for null module");
        } catch(NullPointerException e) {
            // Expected
        }

        try {
            ModuleHelper.AddModuleExportsToAll(m2, null);
            throw new RuntimeException("Failed to get the expected NPE for null package");
        } catch(NullPointerException e) {
            // Expected
        }

        try { // Expect IAE when passing a ClassLoader object instead of a java.lang.reflect.Module object.
            ModuleHelper.AddModuleExportsToAll(this_cldr, "p2");
            throw new RuntimeException("Failed to get the expected IAE for bad module");
        } catch(IllegalArgumentException e) {
            // Expected
        }

        try {
            ModuleHelper.AddModuleExportsToAll(m2, "p3");
            throw new RuntimeException("Failed to get the expected IAE for package that is in another module");
        } catch(IllegalArgumentException e) {
            // Expected
        }

        try {
            ModuleHelper.AddModuleExportsToAll(m2, "p4");
            throw new RuntimeException("Failed to get the expected IAE for package not in any module");
        } catch(IllegalArgumentException e) {
            // Expected
        }

        // Export package p2 in m2 unqualifiedly. Then, do a qualified export
        // of p2 in m2 to m3.  This should not affect the unqualified export.
        m3 = ModuleHelper.ModuleObject("module3", this_cldr, new String[] { "p4" });
        assertNotNull(m3, "Module m3 should not be null");
        ModuleHelper.DefineModule(m3, "9.0", "m3/there", new String[] { "p4" });
        ModuleHelper.AddModuleExportsToAll(m2, "p2");
        ModuleHelper.AddModuleExports(m2, "p2", m3);

        // p1.c1's ctor tries to call a method in p2.c2.  This should succeed because
        // p1 is in an unnamed module and p2.c2 is exported unqualifiedly.
        Class p1_c1_class = Class.forName("p1.c1");
        try {
            Object c1_obj = p1_c1_class.newInstance();
        } catch (IllegalAccessError f) {
            throw new RuntimeException(
                "Class in unnamed module could not access package p2 exported unqualifieldly");
        }
    }
}

