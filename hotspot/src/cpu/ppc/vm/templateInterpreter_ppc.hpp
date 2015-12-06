/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2013, 2015 SAP AG. All rights reserved.
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

#ifndef CPU_PPC_VM_TEMPLATEINTERPRETER_PPC_HPP
#define CPU_PPC_VM_TEMPLATEINTERPRETER_PPC_HPP

 protected:

  // Size of interpreter code. Increase if too small.  Interpreter will
  // fail with a guarantee ("not enough space for interpreter generation");
  // if too small.
  // Run with +PrintInterpreter to get the VM to print out the size.
  // Max size with JVMTI
  const static int InterpreterCodeSize = 230*K;

 public:
  // Support abs and sqrt like in compiler.
  // For others we can use a normal (native) entry.
  static bool math_entry_available(AbstractInterpreter::MethodKind kind);
#endif // CPU_PPC_VM_TEMPLATEINTERPRETER_PPC_HPP


