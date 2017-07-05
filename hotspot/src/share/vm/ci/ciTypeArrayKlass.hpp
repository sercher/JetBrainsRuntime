/*
 * Copyright (c) 1999, 2012, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_VM_CI_CITYPEARRAYKLASS_HPP
#define SHARE_VM_CI_CITYPEARRAYKLASS_HPP

#include "ci/ciArrayKlass.hpp"

// ciTypeArrayKlass
//
// This class represents a Klass* in the HotSpot virtual machine
// whose Klass part in a TypeArrayKlass.
class ciTypeArrayKlass : public ciArrayKlass {
  CI_PACKAGE_ACCESS

protected:
  ciTypeArrayKlass(KlassHandle h_k);

  TypeArrayKlass* get_TypeArrayKlass() {
    return (TypeArrayKlass*)get_Klass();
  }

  const char* type_string() { return "ciTypeArrayKlass"; }

  // Helper method for make.
  static ciTypeArrayKlass* make_impl(BasicType type);

public:
  // The type of the array elements.
  BasicType element_type() {
    return Klass::layout_helper_element_type(layout_helper());
  }

  // What kind of ciObject is this?
  bool is_type_array_klass() const { return true; }

  // Make an array klass corresponding to the specified primitive type.
  static ciTypeArrayKlass* make(BasicType type);

  virtual ciKlass* exact_klass() {
    return this;
  }
};

#endif // SHARE_VM_CI_CITYPEARRAYKLASS_HPP
