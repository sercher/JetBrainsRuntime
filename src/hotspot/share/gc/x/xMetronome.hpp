/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_X_XMETRONOME_HPP
#define SHARE_GC_X_XMETRONOME_HPP

#include "memory/allocation.hpp"
#include "runtime/mutex.hpp"

class XMetronome : public StackObj {
private:
  Monitor        _monitor;
  const uint64_t _interval_ms;
  uint64_t       _start_ms;
  uint64_t       _nticks;
  bool           _stopped;

public:
  XMetronome(uint64_t hz);

  bool wait_for_tick();
  void stop();
};

#endif // SHARE_GC_X_XMETRONOME_HPP
