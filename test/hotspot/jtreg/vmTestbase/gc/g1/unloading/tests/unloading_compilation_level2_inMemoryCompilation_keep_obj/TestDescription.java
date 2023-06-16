/*
 * Copyright (c) 2017, 2022, Oracle and/or its affiliates. All rights reserved.
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
 * @key stress randomness
 *
 * @summary converted from VM Testbase gc/g1/unloading/tests/unloading_compilation_level2_inMemoryCompilation_keep_obj.
 * VM Testbase keywords: [gc, stress, stressopt, nonconcurrent, javac]
 *
 * @modules java.base/jdk.internal.misc
 * @library /vmTestbase
 *          /test/lib
 *
 *
 * @comment generate HumongousTemplateClass and compile it to test.classes
 * @run driver gc.g1.unloading.bytecode.GenClassesBuilder
 *
 * @requires vm.gc.G1
 * @requires vm.opt.ClassUnloading != false
 * @requires vm.opt.ClassUnloadingWithConcurrentMark != false
 * @requires vm.flavor == "server" & (vm.opt.TieredStopAtLevel == null | vm.opt.TieredStopAtLevel >= 2)
 * @build jdk.test.whitebox.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @run main/othervm
 *      -Xbootclasspath/a:.
 *      -XX:+UnlockDiagnosticVMOptions
 *      -XX:+WhiteBoxAPI
 *      -XX:+TieredCompilation
 *      -XX:+UseG1GC
 *      -XX:+ExplicitGCInvokesConcurrent
 *      -Xlog:gc:gc.log
 *      -XX:-UseGCOverheadLimit
 *      gc.g1.unloading.UnloadingTest
 *      -compilationNumber 1
 *      -compilationLevel 2
 *      -inMemoryCompilation
 *      -keep object
 *      -numberOfChecksLimit 4
 *      -stressTime 180
 */

