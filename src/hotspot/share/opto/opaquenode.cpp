/*
 * Copyright (c) 2014, 2023, Oracle and/or its affiliates. All rights reserved.
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
#include "opto/opaquenode.hpp"
#include "opto/phaseX.hpp"

//=============================================================================
// Do not allow value-numbering
uint Opaque1Node::hash() const { return NO_HASH; }
bool Opaque1Node::cmp( const Node &n ) const {
  return (&n == this);          // Always fail except on self
}

//------------------------------Identity---------------------------------------
// Do NOT remove the opaque node until no more loop opts can happen.
Node* Opaque1Node::Identity(PhaseGVN* phase) {
  if (phase->C->post_loop_opts_phase()) {
    return in(1);
  } else {
    phase->C->record_for_post_loop_opts_igvn(this);
  }
  return this;
}

// Do NOT remove the opaque node until no more loop opts can happen.
Node* Opaque3Node::Identity(PhaseGVN* phase) {
  if (phase->C->post_loop_opts_phase()) {
    return in(1);
  } else {
    phase->C->record_for_post_loop_opts_igvn(this);
  }
  return this;
}

// Do not allow value-numbering
uint Opaque3Node::hash() const { return NO_HASH; }
bool Opaque3Node::cmp(const Node &n) const {
  return (&n == this);          // Always fail except on self
}

const Type* Opaque4Node::Value(PhaseGVN* phase) const {
  return phase->type(in(1));
}

//=============================================================================

uint ProfileBooleanNode::hash() const { return NO_HASH; }
bool ProfileBooleanNode::cmp( const Node &n ) const {
  return (&n == this);
}

Node *ProfileBooleanNode::Ideal(PhaseGVN *phase, bool can_reshape) {
  if (can_reshape && _delay_removal) {
    _delay_removal = false;
    return this;
  } else {
    return nullptr;
  }
}

Node* ProfileBooleanNode::Identity(PhaseGVN* phase) {
  if (_delay_removal) {
    return this;
  } else {
    assert(_consumed, "profile should be consumed before elimination");
    return in(1);
  }
}
