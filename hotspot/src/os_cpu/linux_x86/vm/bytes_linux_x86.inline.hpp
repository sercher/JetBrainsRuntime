/*
 * Copyright 1999-2001 Sun Microsystems, Inc.  All Rights Reserved.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 *
 */

#include <byteswap.h>

// Efficient swapping of data bytes from Java byte
// ordering to native byte ordering and vice versa.
inline u2   Bytes::swap_u2(u2 x) {
#ifdef AMD64
  return bswap_16(x);
#else
  u2 ret;
  __asm__ __volatile__ (
    "movw %0, %%ax;"
    "xchg %%al, %%ah;"
    "movw %%ax, %0"
    :"=r" (ret)      // output : register 0 => ret
    :"0"  (x)        // input  : x => register 0
    :"ax", "0"       // clobbered registers
  );
  return ret;
#endif // AMD64
}

inline u4   Bytes::swap_u4(u4 x) {
#ifdef AMD64
  return bswap_32(x);
#else
  u4 ret;
  __asm__ __volatile__ (
    "bswap %0"
    :"=r" (ret)      // output : register 0 => ret
    :"0"  (x)        // input  : x => register 0
    :"0"             // clobbered register
  );
  return ret;
#endif // AMD64
}

#ifdef AMD64
inline u8 Bytes::swap_u8(u8 x) {
#ifdef SPARC_WORKS
  // workaround for SunStudio12 CR6615391
  __asm__ __volatile__ (
    "bswapq %0"
    :"=r" (x)        // output : register 0 => x
    :"0"  (x)        // input  : x => register 0
    :"0"             // clobbered register
  );
  return x;
#else
  return bswap_64(x);
#endif
}
#else
// Helper function for swap_u8
inline u8   Bytes::swap_u8_base(u4 x, u4 y) {
  return (((u8)swap_u4(x))<<32) | swap_u4(y);
}

inline u8 Bytes::swap_u8(u8 x) {
  return swap_u8_base(*(u4*)&x, *(((u4*)&x)+1));
}
#endif // !AMD64
