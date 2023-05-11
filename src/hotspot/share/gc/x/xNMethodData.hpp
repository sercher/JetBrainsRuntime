/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_X_XNMETHODDATA_HPP
#define SHARE_GC_X_XNMETHODDATA_HPP

#include "gc/x/xAttachedArray.hpp"
#include "gc/x/xLock.hpp"
#include "memory/allocation.hpp"
#include "oops/oopsHierarchy.hpp"
#include "utilities/globalDefinitions.hpp"

class nmethod;
template <typename T> class GrowableArray;

class XNMethodDataOops {
private:
  typedef XAttachedArray<XNMethodDataOops, oop*> AttachedArray;

  const AttachedArray _immediates;
  const bool          _has_non_immediates;

  XNMethodDataOops(const GrowableArray<oop*>& immediates, bool has_non_immediates);

public:
  static XNMethodDataOops* create(const GrowableArray<oop*>& immediates, bool has_non_immediates);
  static void destroy(XNMethodDataOops* oops);

  size_t immediates_count() const;
  oop** immediates_begin() const;
  oop** immediates_end() const;

  bool has_non_immediates() const;
};

class XNMethodData : public CHeapObj<mtGC> {
private:
  XReentrantLock             _lock;
  XNMethodDataOops* volatile _oops;

public:
  XNMethodData();
  ~XNMethodData();

  XReentrantLock* lock();

  XNMethodDataOops* oops() const;
  XNMethodDataOops* swap_oops(XNMethodDataOops* oops);
};

#endif // SHARE_GC_X_XNMETHODDATA_HPP
