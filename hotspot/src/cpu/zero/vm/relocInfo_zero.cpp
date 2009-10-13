/*
 * Copyright 2003-2005 Sun Microsystems, Inc.  All Rights Reserved.
 * Copyright 2007, 2009 Red Hat, Inc.
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

#include "incls/_precompiled.incl"
#include "incls/_relocInfo_zero.cpp.incl"

void Relocation::pd_set_data_value(address x, intptr_t o) {
  ShouldNotCallThis();
}

address Relocation::pd_call_destination(address orig_addr) {
  ShouldNotCallThis();
}

void Relocation::pd_set_call_destination(address x) {
  ShouldNotCallThis();
}

address Relocation::pd_get_address_from_code() {
  ShouldNotCallThis();
}

address* Relocation::pd_address_in_code() {
  // Relocations in Shark are just stored directly
  return (address *) addr();
}

int Relocation::pd_breakpoint_size() {
  ShouldNotCallThis();
}

void Relocation::pd_swap_in_breakpoint(address x,
                                       short*  instrs,
                                       int     instrlen) {
  ShouldNotCallThis();
}

void Relocation::pd_swap_out_breakpoint(address x,
                                        short*  instrs,
                                        int     instrlen) {
  ShouldNotCallThis();
}

void poll_Relocation::fix_relocation_after_move(const CodeBuffer* src,
                                                CodeBuffer*       dst) {
  ShouldNotCallThis();
}

void poll_return_Relocation::fix_relocation_after_move(const CodeBuffer* src,
                                                       CodeBuffer*       dst) {
  ShouldNotCallThis();
}
