/*
 * Copyright (c) 2013, 2023, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_UTILITIES_JFRSPINLOCKHELPER_HPP
#define SHARE_JFR_UTILITIES_JFRSPINLOCKHELPER_HPP

#include "runtime/javaThread.hpp"

// this utility could be useful for non cx8 platforms

class JfrSpinlockHelper {
 private:
  volatile int* const _lock;

 public:
  JfrSpinlockHelper(volatile int* lock) : _lock(lock) {
    Thread::SpinAcquire(_lock, nullptr);
  }

  JfrSpinlockHelper(volatile int* const lock, const char* name) : _lock(lock) {
    Thread::SpinAcquire(_lock, name);
  }

  ~JfrSpinlockHelper() {
    Thread::SpinRelease(_lock);
  }
};

#endif // SHARE_JFR_UTILITIES_JFRSPINLOCKHELPER_HPP
