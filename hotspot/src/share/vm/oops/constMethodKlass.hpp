/*
 * Copyright (c) 2003, 2012, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_VM_OOPS_CONSTMETHODKLASS_HPP
#define SHARE_VM_OOPS_CONSTMETHODKLASS_HPP

#include "oops/klass.hpp"
#include "oops/oop.hpp"
#include "runtime/orderAccess.hpp"

// A constMethodKlass is the klass of a constMethodOop

class constMethodKlass : public Klass {
  friend class VMStructs;
private:
  juint    _alloc_size;        // allocation profiling support
public:
  // Testing
  bool oop_is_constMethod() const { return true; }
  virtual bool oop_is_parsable(oop obj) const;
  virtual bool oop_is_conc_safe(oop obj) const;


  // Allocation
  DEFINE_ALLOCATE_PERMANENT(constMethodKlass);
  constMethodOop allocate(int byte_code_size, int compressed_line_number_size,
                          int localvariable_table_length,
                          int exception_table_length,
                          int checked_exceptions_length,
                          bool is_conc_safe,
                          TRAPS);
  static klassOop create_klass(TRAPS);

  // Sizing
  int oop_size(oop obj) const;
  int klass_oop_size() const     { return object_size(); }

  // Casting from klassOop
  static constMethodKlass* cast(klassOop k) {
    assert(k->klass_part()->oop_is_constMethod(), "cast to constMethodKlass");
    return (constMethodKlass*) k->klass_part();
  }

  // Sizing
  static int header_size() {
    return oopDesc::header_size() + sizeof(constMethodKlass)/HeapWordSize;
  }
  int object_size() const {
    return align_object_size(header_size());
  }

  // Garbage collection
  void oop_follow_contents(oop obj);
  int  oop_adjust_pointers(oop obj);

  // Parallel Scavenge and Parallel Old
  PARALLEL_GC_DECLS

  // Allocation profiling support
  juint alloc_size() const              { return _alloc_size; }
  void set_alloc_size(juint n)          { _alloc_size = n; }

  // Iterators
  int oop_oop_iterate(oop obj, OopClosure* blk);
  int oop_oop_iterate_m(oop obj, OopClosure* blk, MemRegion mr);

  // Printing
  void oop_print_value_on(oop obj, outputStream* st);
  void oop_print_on      (oop obj, outputStream* st);

  // Verify operations
  const char* internal_name() const;
  void oop_verify_on(oop obj, outputStream* st);
  bool oop_partially_loaded(oop obj) const;
  void oop_set_partially_loaded(oop obj);
};

#endif // SHARE_VM_OOPS_CONSTMETHODKLASS_HPP
