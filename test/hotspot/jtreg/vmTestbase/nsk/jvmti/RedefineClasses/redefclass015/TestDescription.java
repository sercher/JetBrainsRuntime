/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @summary converted from VM Testbase nsk/jvmti/RedefineClasses/redefclass015.
 * VM Testbase keywords: [quick, jpda, jvmti, noras, redefine]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test exercises JVMTI function RedefineClasses(classCount, classDefs).
 *     The test checks if the function returns:
 *       - JVMTI_ERROR_FAILS_VERIFICATION if new class is wrong.
 * COMMENTS
 *     Ported from JVMDI.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.RedefineClasses.redefclass015
 *
 * @comment compile newclassXX/*.jasm to bin/newclassXX
 * @compile newclass/redefclass015a.jasm
 * @run driver
 *      nsk.jdi.ExtraClassesInstaller
 *      newclass nsk/jvmti/RedefineClasses/redefclass015a.klass
 *
 * @run main/othervm/native -agentlib:redefclass015 nsk.jvmti.RedefineClasses.redefclass015 ./bin
 */

