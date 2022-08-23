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
 * @summary converted from VM Testbase nsk/jdi/TypeComponent/isFinal/isfinal003.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test checks the isFinal() method of TypeComponent interface of
 *     com.sun.jdi package.
 *     The method spec:
 *     public boolean isFinal()
 *     Determines if this TypeComponent is final. Return value is undefined for
 *     constructors and static initializers.
 *     Returns: true if this type component was declared final; false otherwise.
 *     nsk/jdi/TypeComponent/isFinal/isfinal003 checks assertion:
 *     public boolean isFinal()
 *     1. method isFinal() is defined for constructors and static initializers and
 *        does not throw an Exception.
 *     Debugger gets all methods from debuggee, finds contructors and static
 *     initializers only and checks if isFinal() doesn't throw any Exception.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.TypeComponent.isFinal.isfinal003
 *        nsk.jdi.TypeComponent.isFinal.isfinal003a
 * @run main/othervm
 *      nsk.jdi.TypeComponent.isFinal.isfinal003
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

