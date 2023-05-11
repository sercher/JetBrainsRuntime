/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_X_XDRIVER_HPP
#define SHARE_GC_X_XDRIVER_HPP

#include "gc/shared/concurrentGCThread.hpp"
#include "gc/shared/gcCause.hpp"
#include "gc/x/xMessagePort.hpp"

class VM_XOperation;

class XDriverRequest {
private:
  GCCause::Cause _cause;
  uint           _nworkers;

public:
  XDriverRequest();
  XDriverRequest(GCCause::Cause cause);
  XDriverRequest(GCCause::Cause cause, uint nworkers);

  bool operator==(const XDriverRequest& other) const;

  GCCause::Cause cause() const;
  uint nworkers() const;
};

class XDriver : public ConcurrentGCThread {
private:
  XMessagePort<XDriverRequest> _gc_cycle_port;
  XRendezvousPort              _gc_locker_port;

  template <typename T> bool pause();

  void pause_mark_start();
  void concurrent_mark();
  bool pause_mark_end();
  void concurrent_mark_continue();
  void concurrent_mark_free();
  void concurrent_process_non_strong_references();
  void concurrent_reset_relocation_set();
  void pause_verify();
  void concurrent_select_relocation_set();
  void pause_relocate_start();
  void concurrent_relocate();

  void check_out_of_memory();

  void gc(const XDriverRequest& request);

protected:
  virtual void run_service();
  virtual void stop_service();

public:
  XDriver();

  bool is_busy() const;

  void collect(const XDriverRequest& request);
};

#endif // SHARE_GC_X_XDRIVER_HPP
