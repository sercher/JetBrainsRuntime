/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2014, 2020, Red Hat Inc. All rights reserved.
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

#include "precompiled.hpp"
#include "asm/macroAssembler.inline.hpp"
#include "interpreter/interp_masm.hpp"
#include "interpreter/interpreter.hpp"
#include "interpreter/interpreterRuntime.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/universe.hpp"
#include "oops/method.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/icache.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/signature.hpp"

#define __ _masm->

// Implementation of SignatureHandlerGenerator
Register InterpreterRuntime::SignatureHandlerGenerator::from() { return rlocals; }
Register InterpreterRuntime::SignatureHandlerGenerator::to()   { return sp; }
Register InterpreterRuntime::SignatureHandlerGenerator::temp() { return rscratch1; }

Register InterpreterRuntime::SignatureHandlerGenerator::next_gpr() {
  if (_num_reg_int_args < Argument::n_int_register_parameters_c-1) {
    return as_Register(_num_reg_int_args++ + c_rarg1->encoding());
  }
  return noreg;
}

FloatRegister InterpreterRuntime::SignatureHandlerGenerator::next_fpr() {
  if (_num_reg_fp_args < Argument::n_float_register_parameters_c) {
    return as_FloatRegister(_num_reg_fp_args++);
  }
  return fnoreg;
}

int InterpreterRuntime::SignatureHandlerGenerator::next_stack_offset() {
  int ret = _stack_offset;
  _stack_offset += wordSize;
  return ret;
}

InterpreterRuntime::SignatureHandlerGenerator::SignatureHandlerGenerator(
      const methodHandle& method, CodeBuffer* buffer) : NativeSignatureIterator(method) {
  _masm = new MacroAssembler(buffer);
  _num_reg_int_args = (method->is_static() ? 1 : 0);
  _num_reg_fp_args = 0;
  _stack_offset = 0;
}

void InterpreterRuntime::SignatureHandlerGenerator::pass_int() {
  const Address src(from(), Interpreter::local_offset_in_bytes(offset()));

  Register reg = next_gpr();
  if (reg != noreg) {
    __ ldr(reg, src);
  } else {
    __ ldrw(r0, src);
    __ strw(r0, Address(to(), next_stack_offset()));
  }
}

void InterpreterRuntime::SignatureHandlerGenerator::pass_long() {
  const Address src(from(), Interpreter::local_offset_in_bytes(offset() + 1));

  Register reg = next_gpr();
  if (reg != noreg) {
    __ ldr(reg, src);
  } else {
    __ ldr(r0, src);
    __ str(r0, Address(to(), next_stack_offset()));
  }
}

void InterpreterRuntime::SignatureHandlerGenerator::pass_float() {
  const Address src(from(), Interpreter::local_offset_in_bytes(offset()));

  FloatRegister reg = next_fpr();
  if (reg != fnoreg) {
    __ ldrs(reg, src);
  } else {
    __ ldrw(r0, src);
    __ strw(r0, Address(to(), next_stack_offset()));
  }
}

void InterpreterRuntime::SignatureHandlerGenerator::pass_double() {
  const Address src(from(), Interpreter::local_offset_in_bytes(offset() + 1));

  FloatRegister reg = next_fpr();
  if (reg != fnoreg) {
    __ ldrd(reg, src);
  } else {
    __ ldr(r0, src);
    __ str(r0, Address(to(), next_stack_offset()));
  }
}

void InterpreterRuntime::SignatureHandlerGenerator::pass_object() {
  Register reg = next_gpr();
  if (reg == c_rarg1) {
    assert(offset() == 0, "argument register 1 can only be (non-null) receiver");
    __ add(c_rarg1, from(), Interpreter::local_offset_in_bytes(offset()));
  } else if (reg != noreg) {
    __ add(r0, from(), Interpreter::local_offset_in_bytes(offset()));
    __ mov(reg, 0);
    __ ldr(temp(), r0);
    Label L;
    __ cbz(temp(), L);
    __ mov(reg, r0);
    __ bind(L);
  } else {
    __ add(r0, from(), Interpreter::local_offset_in_bytes(offset()));
    __ ldr(temp(), r0);
    Label L;
    __ cbnz(temp(), L);
    __ mov(r0, zr);
    __ bind(L);
    __ str(r0, Address(to(), next_stack_offset()));
  }
}

void InterpreterRuntime::SignatureHandlerGenerator::generate(uint64_t fingerprint) {
  // generate code to handle arguments
  iterate(fingerprint);

  // return result handler
  __ lea(r0, ExternalAddress(Interpreter::result_handler(method()->result_type())));
  __ ret(lr);

  __ flush();
}


// Implementation of SignatureHandlerLibrary

void SignatureHandlerLibrary::pd_set_handler(address handler) {}


class SlowSignatureHandler
  : public NativeSignatureIterator {
 private:
  address   _from;
  intptr_t* _to;
  intptr_t* _int_args;
  intptr_t* _fp_args;
  intptr_t* _fp_identifiers;
  unsigned int _num_reg_int_args;
  unsigned int _num_reg_fp_args;

  intptr_t* single_slot_addr() {
    intptr_t* from_addr = (intptr_t*)(_from+Interpreter::local_offset_in_bytes(0));
    _from -= Interpreter::stackElementSize;
    return from_addr;
  }

  intptr_t* double_slot_addr() {
    intptr_t* from_addr = (intptr_t*)(_from+Interpreter::local_offset_in_bytes(1));
    _from -= 2*Interpreter::stackElementSize;
    return from_addr;
  }

  int pass_gpr(intptr_t value) {
    if (_num_reg_int_args < Argument::n_int_register_parameters_c-1) {
      *_int_args++ = value;
      return _num_reg_int_args++;
    }
    return -1;
  }

  int pass_fpr(intptr_t value) {
    if (_num_reg_fp_args < Argument::n_float_register_parameters_c) {
      *_fp_args++ = value;
      return _num_reg_fp_args++;
    }
    return -1;
  }

  void pass_stack(intptr_t value) {
    *_to++ = value;
  }

  virtual void pass_int() {
    jint value = *(jint*)single_slot_addr();
    if (pass_gpr(value) < 0) {
      pass_stack(value);
    }
  }

  virtual void pass_long() {
    intptr_t value = *double_slot_addr();
    if (pass_gpr(value) < 0) {
      pass_stack(value);
    }
  }

  virtual void pass_object() {
    intptr_t* addr = single_slot_addr();
    intptr_t value = *addr == 0 ? NULL : (intptr_t)addr;
    if (pass_gpr(value) < 0) {
      pass_stack(value);
    }
  }

  virtual void pass_float() {
    jint value = *(jint*)single_slot_addr();
    if (pass_fpr(value) < 0) {
      pass_stack(value);
    }
  }

  virtual void pass_double() {
    intptr_t value = *double_slot_addr();
    int arg = pass_fpr(value);
    if (0 <= arg) {
      *_fp_identifiers |= (1ull << arg); // mark as double
    } else {
      pass_stack(value);
    }
  }

 public:
  SlowSignatureHandler(const methodHandle& method, address from, intptr_t* to)
    : NativeSignatureIterator(method)
  {
    _from = from;
    _to   = to;

    _int_args = to - (method->is_static() ? 16 : 17);
    _fp_args =  to - 8;
    _fp_identifiers = to - 9;
    *(int*) _fp_identifiers = 0;
    _num_reg_int_args = (method->is_static() ? 1 : 0);
    _num_reg_fp_args = 0;
  }

};


IRT_ENTRY(address,
          InterpreterRuntime::slow_signature_handler(JavaThread* thread,
                                                     Method* method,
                                                     intptr_t* from,
                                                     intptr_t* to))
  methodHandle m(thread, (Method*)method);
  assert(m->is_native(), "sanity check");

  // handle arguments
  SlowSignatureHandler ssh(m, (address)from, to);
  ssh.iterate((uint64_t)CONST64(-1));

  // return result handler
  return Interpreter::result_handler(m->result_type());
IRT_END
