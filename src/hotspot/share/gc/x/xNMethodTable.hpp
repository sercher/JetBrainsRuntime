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

#ifndef SHARE_GC_X_XNMETHODTABLE_HPP
#define SHARE_GC_X_XNMETHODTABLE_HPP

#include "gc/x/xNMethodTableIteration.hpp"
#include "gc/x/xSafeDelete.hpp"
#include "memory/allStatic.hpp"

class nmethod;
class NMethodClosure;
class XNMethodTableEntry;
class XWorkers;

class XNMethodTable : public AllStatic {
private:
  static XNMethodTableEntry*                     _table;
  static size_t                                  _size;
  static size_t                                  _nregistered;
  static size_t                                  _nunregistered;
  static XNMethodTableIteration                  _iteration;
  static XSafeDeleteNoLock<XNMethodTableEntry[]> _safe_delete;

  static XNMethodTableEntry* create(size_t size);
  static void destroy(XNMethodTableEntry* table);

  static size_t first_index(const nmethod* nm, size_t size);
  static size_t next_index(size_t prev_index, size_t size);

  static bool register_entry(XNMethodTableEntry* table, size_t size, nmethod* nm);
  static void unregister_entry(XNMethodTableEntry* table, size_t size, nmethod* nm);

  static void rebuild(size_t new_size);
  static void rebuild_if_needed();

public:
  static size_t registered_nmethods();
  static size_t unregistered_nmethods();

  static void register_nmethod(nmethod* nm);
  static void unregister_nmethod(nmethod* nm);

  static void wait_until_iteration_done();

  static void nmethods_do_begin();
  static void nmethods_do_end();
  static void nmethods_do(NMethodClosure* cl);

  static void unlink(XWorkers* workers, bool unloading_occurred);
  static void purge(XWorkers* workers);
};

#endif // SHARE_GC_X_XNMETHODTABLE_HPP
