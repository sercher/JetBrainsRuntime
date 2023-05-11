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

#ifndef SHARE_GC_X_XLARGEPAGES_HPP
#define SHARE_GC_X_XLARGEPAGES_HPP

#include "memory/allStatic.hpp"

class XLargePages : public AllStatic {
private:
  enum State {
    Disabled,
    Explicit,
    Transparent
  };

  static State _state;

  static void pd_initialize();

public:
  static void initialize();

  static bool is_enabled();
  static bool is_explicit();
  static bool is_transparent();

  static const char* to_string();
};

#endif // SHARE_GC_X_XLARGEPAGES_HPP
