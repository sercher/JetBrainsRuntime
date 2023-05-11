/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_X_XTHREADLOCALALLOCBUFFER_HPP
#define SHARE_GC_X_XTHREADLOCALALLOCBUFFER_HPP

#include "gc/shared/threadLocalAllocBuffer.hpp"
#include "gc/x/xValue.hpp"
#include "memory/allStatic.hpp"

class JavaThread;

class XThreadLocalAllocBuffer : public AllStatic {
private:
  static XPerWorker<ThreadLocalAllocStats>* _stats;

public:
  static void initialize();

  static void reset_statistics();
  static void publish_statistics();

  static void retire(JavaThread* thread, ThreadLocalAllocStats* stats);
  static void remap(JavaThread* thread);
  static void update_stats(JavaThread* thread);
};

#endif // SHARE_GC_X_XTHREADLOCALALLOCBUFFER_HPP
