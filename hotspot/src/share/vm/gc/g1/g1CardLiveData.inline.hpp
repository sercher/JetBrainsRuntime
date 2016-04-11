/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_VM_GC_G1_G1CARDLIVEDATA_INLINE_HPP
#define SHARE_VM_GC_G1_G1CARDLIVEDATA_INLINE_HPP

#include "gc/g1/g1CardLiveData.hpp"
#include "utilities/bitMap.inline.hpp"
#include "utilities/globalDefinitions.hpp"

inline BitMap G1CardLiveData::live_card_bitmap(uint region) {
  return BitMap(_live_cards + ((size_t)region * _cards_per_region >> LogBitsPerWord), _cards_per_region);
}

inline bool G1CardLiveData::is_card_live_at(BitMap::idx_t idx) const {
  return live_cards_bm().at(idx);
}

inline bool G1CardLiveData::is_region_live(uint region) const {
  return live_regions_bm().at(region);
}

inline void G1CardLiveData::remove_nonlive_cards(uint region, BitMap* bm) {
  bm->set_intersection(live_card_bitmap(region));
}

inline void G1CardLiveData::remove_nonlive_regions(BitMap* bm) {
  bm->set_intersection(live_regions_bm());
}

#endif /* SHARE_VM_GC_G1_G1CARDLIVEDATA_INLINE_HPP */
