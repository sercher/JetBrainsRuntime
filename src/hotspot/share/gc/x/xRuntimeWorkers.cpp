/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include "precompiled.hpp"
#include "gc/shared/gcLogPrecious.hpp"
#include "gc/shared/gc_globals.hpp"
#include "gc/x/xLock.inline.hpp"
#include "gc/x/xRuntimeWorkers.hpp"
#include "gc/x/xTask.hpp"
#include "gc/x/xThread.hpp"
#include "runtime/java.hpp"

class XRuntimeWorkersInitializeTask : public WorkerTask {
private:
  const uint     _nworkers;
  uint           _started;
  XConditionLock _lock;

public:
  XRuntimeWorkersInitializeTask(uint nworkers) :
      WorkerTask("XRuntimeWorkersInitializeTask"),
      _nworkers(nworkers),
      _started(0),
      _lock() {}

  virtual void work(uint worker_id) {
    // Wait for all threads to start
    XLocker<XConditionLock> locker(&_lock);
    if (++_started == _nworkers) {
      // All threads started
      _lock.notify_all();
    } else {
      while (_started != _nworkers) {
        _lock.wait();
      }
    }
  }
};

XRuntimeWorkers::XRuntimeWorkers() :
    _workers("RuntimeWorker",
             ParallelGCThreads) {

  log_info_p(gc, init)("Runtime Workers: %u", _workers.max_workers());

  // Initialize worker threads
  _workers.initialize_workers();
  _workers.set_active_workers(_workers.max_workers());
  if (_workers.active_workers() != _workers.max_workers()) {
    vm_exit_during_initialization("Failed to create XRuntimeWorkers");
  }

  // Execute task to reduce latency in early safepoints,
  // which otherwise would have to take on any warmup costs.
  XRuntimeWorkersInitializeTask task(_workers.max_workers());
  _workers.run_task(&task);
}

WorkerThreads* XRuntimeWorkers::workers() {
  return &_workers;
}

void XRuntimeWorkers::threads_do(ThreadClosure* tc) const {
  _workers.threads_do(tc);
}
