/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, 2022, Huawei Technologies Co., Ltd. All rights reserved.
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

#ifndef CPU_RISCV_GLOBALS_RISCV_HPP
#define CPU_RISCV_GLOBALS_RISCV_HPP

#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"

// Sets the default values for platform dependent flags used by the runtime system.
// (see globals.hpp)

define_pd_global(bool, ImplicitNullChecks,       true);  // Generate code for implicit null checks
define_pd_global(bool, TrapBasedNullChecks,      false);
define_pd_global(bool, UncommonNullCast,         true);  // Uncommon-trap NULLs past to check cast

define_pd_global(uintx, CodeCacheSegmentSize,    64 COMPILER1_AND_COMPILER2_PRESENT(+64)); // Tiered compilation has large code-entry alignment.
define_pd_global(intx, CodeEntryAlignment,       64);
define_pd_global(intx, OptoLoopAlignment,        16);
define_pd_global(intx, InlineFrequencyCount,     100);

#define DEFAULT_STACK_YELLOW_PAGES (2)
#define DEFAULT_STACK_RED_PAGES (1)
// Java_java_net_SocketOutputStream_socketWrite0() uses a 64k buffer on the
// stack if compiled for unix and LP64. To pass stack overflow tests we need
// 20 shadow pages.
#define DEFAULT_STACK_SHADOW_PAGES (20 DEBUG_ONLY(+5))
#define DEFAULT_STACK_RESERVED_PAGES (1)

#define MIN_STACK_YELLOW_PAGES DEFAULT_STACK_YELLOW_PAGES
#define MIN_STACK_RED_PAGES    DEFAULT_STACK_RED_PAGES
#define MIN_STACK_SHADOW_PAGES DEFAULT_STACK_SHADOW_PAGES
#define MIN_STACK_RESERVED_PAGES (0)

define_pd_global(intx, StackYellowPages, DEFAULT_STACK_YELLOW_PAGES);
define_pd_global(intx, StackRedPages, DEFAULT_STACK_RED_PAGES);
define_pd_global(intx, StackShadowPages, DEFAULT_STACK_SHADOW_PAGES);
define_pd_global(intx, StackReservedPages, DEFAULT_STACK_RESERVED_PAGES);

define_pd_global(bool, RewriteBytecodes,     true);
define_pd_global(bool, RewriteFrequentPairs, true);

define_pd_global(bool, PreserveFramePointer, false);

define_pd_global(uintx, TypeProfileLevel, 111);

define_pd_global(bool, CompactStrings, true);

// Clear short arrays bigger than one word in an arch-specific way
define_pd_global(intx, InitArrayShortSize, BytesPerLong);

define_pd_global(intx, InlineSmallCode,          1000);

#define ARCH_FLAGS(develop,                                                      \
                   product,                                                      \
                   notproduct,                                                   \
                   range,                                                        \
                   constraint)                                                   \
                                                                                 \
  product(bool, NearCpool, true,                                                 \
         "constant pool is close to instructions")                               \
  product(intx, BlockZeroingLowLimit, 256,                                       \
          "Minimum size in bytes when block zeroing will be used")               \
          range(1, max_jint)                                                     \
  product(bool, TraceTraps, false, "Trace all traps the signal handler")         \
  /* For now we're going to be safe and add the I/O bits to userspace fences. */ \
  product(bool, UseConservativeFence, true,                                      \
          "Extend i for r and o for w in the pred/succ flags of fence")          \
  product(bool, AvoidUnalignedAccesses, true,                                    \
          "Avoid generating unaligned memory accesses")                          \
  product(bool, UseRVC, true,  "Use RVC instructions")                           \
  product(bool, UseRVV, false, EXPERIMENTAL, "Use RVV instructions")             \
  product(bool, UseZba, false, EXPERIMENTAL, "Use Zba instructions")             \
  product(bool, UseZbb, false, EXPERIMENTAL, "Use Zbb instructions")             \
  product(bool, UseZbs, false, EXPERIMENTAL, "Use Zbs instructions")             \
  product(bool, UseRVVForBigIntegerShiftIntrinsics, true,                        \
          "Use RVV instructions for left/right shift of BigInteger")

#endif // CPU_RISCV_GLOBALS_RISCV_HPP
