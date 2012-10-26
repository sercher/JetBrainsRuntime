/*
 * Copyright (c) 1997, 2012, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_VM_RUNTIME_STUBCODEGENERATOR_HPP
#define SHARE_VM_RUNTIME_STUBCODEGENERATOR_HPP

#include "asm/assembler.hpp"
#include "memory/allocation.hpp"

// All the basic framework for stubcode generation/debugging/printing.


// A StubCodeDesc describes a piece of generated code (usually stubs).
// This information is mainly useful for debugging and printing.
// Currently, code descriptors are simply chained in a linked list,
// this may have to change if searching becomes too slow.

class StubCodeDesc: public CHeapObj<mtCode> {
 protected:
  static StubCodeDesc* _list;                  // the list of all descriptors
  static int           _count;                 // length of list

  StubCodeDesc*        _next;                  // the next element in the linked list
  const char*          _group;                 // the group to which the stub code belongs
  const char*          _name;                  // the name assigned to the stub code
  int                  _index;                 // serial number assigned to the stub
  address              _begin;                 // points to the first byte of the stub code    (included)
  address              _end;                   // points to the first byte after the stub code (excluded)

  void set_end(address end) {
    assert(_begin <= end, "begin & end not properly ordered");
    _end = end;
  }

  void set_begin(address begin) {
    assert(begin >= _begin, "begin may not decrease");
    assert(_end == NULL || begin <= _end, "begin & end not properly ordered");
    _begin = begin;
  }

  friend class StubCodeMark;
  friend class StubCodeGenerator;

 public:
  static StubCodeDesc* desc_for(address pc);     // returns the code descriptor for the code containing pc or NULL
  static StubCodeDesc* desc_for_index(int);      // returns the code descriptor for the index or NULL
  static const char*   name_for(address pc);     // returns the name of the code containing pc or NULL

  StubCodeDesc(const char* group, const char* name, address begin) {
    assert(name != NULL, "no name specified");
    _next           = _list;
    _group          = group;
    _name           = name;
    _index          = ++_count; // (never zero)
    _begin          = begin;
    _end            = NULL;
    _list           = this;
  };

  const char* group() const                      { return _group; }
  const char* name() const                       { return _name; }
  int         index() const                      { return _index; }
  address     begin() const                      { return _begin; }
  address     end() const                        { return _end; }
  int         size_in_bytes() const              { return _end - _begin; }
  bool        contains(address pc) const         { return _begin <= pc && pc < _end; }
  void        print_on(outputStream* st) const;
  void        print() const                      { print_on(tty); }
};

// The base class for all stub-generating code generators.
// Provides utility functions.

class StubCodeGenerator: public StackObj {
 protected:
  MacroAssembler*  _masm;

  StubCodeDesc* _first_stub;
  StubCodeDesc* _last_stub;
  bool _print_code;

 public:
  StubCodeGenerator(CodeBuffer* code, bool print_code = false);
  ~StubCodeGenerator();

  MacroAssembler* assembler() const              { return _masm; }

  virtual void stub_prolog(StubCodeDesc* cdesc); // called by StubCodeMark constructor
  virtual void stub_epilog(StubCodeDesc* cdesc); // called by StubCodeMark destructor
};


// Stack-allocated helper class used to assciate a stub code with a name.
// All stub code generating functions that use a StubCodeMark will be registered
// in the global StubCodeDesc list and the generated stub code can be identified
// later via an address pointing into it.

class StubCodeMark: public StackObj {
 protected:
  StubCodeGenerator* _cgen;
  StubCodeDesc*      _cdesc;

 public:
  StubCodeMark(StubCodeGenerator* cgen, const char* group, const char* name);
  ~StubCodeMark();

};

#endif // SHARE_VM_RUNTIME_STUBCODEGENERATOR_HPP
