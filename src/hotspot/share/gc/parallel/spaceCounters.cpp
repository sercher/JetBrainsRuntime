/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/parallel/spaceCounters.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/atomic.hpp"
#include "runtime/mutex.hpp"
#include "runtime/mutexLocker.hpp"
#include "utilities/debug.hpp"
#include "utilities/macros.hpp"

SpaceCounters::SpaceCounters(const char* name, int ordinal, size_t max_size,
                             MutableSpace* m, GenerationCounters* gc) :
   _object_space(m) {

  if (UsePerfData) {
    EXCEPTION_MARK;
    ResourceMark rm;

    const char* cns = PerfDataManager::name_space(gc->name_space(), "space",
                                                  ordinal);

    _name_space = NEW_C_HEAP_ARRAY(char, strlen(cns)+1, mtGC);
    strcpy(_name_space, cns);

    const char* cname = PerfDataManager::counter_name(_name_space, "name");
    PerfDataManager::create_string_constant(SUN_GC, cname, name, CHECK);

    cname = PerfDataManager::counter_name(_name_space, "maxCapacity");
    PerfDataManager::create_constant(SUN_GC, cname, PerfData::U_Bytes,
                                     (jlong)max_size, CHECK);

    cname = PerfDataManager::counter_name(_name_space, "capacity");
    _capacity = PerfDataManager::create_variable(SUN_GC, cname,
                                   PerfData::U_Bytes,
                                   _object_space->capacity_in_bytes(), CHECK);

    cname = PerfDataManager::counter_name(_name_space, "used");
    _used = PerfDataManager::create_variable(SUN_GC, cname, PerfData::U_Bytes,
                                    new MutableSpaceUsedHelper(_object_space),
                                    CHECK);

    cname = PerfDataManager::counter_name(_name_space, "initCapacity");
    PerfDataManager::create_constant(SUN_GC, cname, PerfData::U_Bytes,
                                     _object_space->capacity_in_bytes(), CHECK);
  }
}

SpaceCounters::~SpaceCounters() {
  FREE_C_HEAP_ARRAY(char, _name_space);
}

static volatile size_t last_used_in_bytes = 0;

void SpaceCounters::update_used() {
  size_t new_used = _object_space->used_in_bytes();
  Atomic::store(&last_used_in_bytes, new_used);
  _used->set_value(new_used);
}

jlong MutableSpaceUsedHelper::take_sample() {
  // Sampling may occur during GC, possibly while GC is updating the space.
  // The space can be in an inconsistent state during such an update.  We
  // don't want to block sampling for the duration of a GC.  Instead, skip
  // sampling in that case, using the last recorded value.
  assert(!Heap_lock->owned_by_self(), "precondition");
  if (Heap_lock->try_lock()) {
    Atomic::store(&last_used_in_bytes, _m->used_in_bytes());
    Heap_lock->unlock();
  }
  return Atomic::load(&last_used_in_bytes);
}
