/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
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
#include "compiler/compileLog.hpp"
#include "libadt/vectset.hpp"
#include "memory/allocation.inline.hpp"
#include "opto/addnode.hpp"
#include "opto/callnode.hpp"
#include "opto/castnode.hpp"
#include "opto/convertnode.hpp"
#include "opto/divnode.hpp"
#include "opto/matcher.hpp"
#include "opto/memnode.hpp"
#include "opto/mulnode.hpp"
#include "opto/opcodes.hpp"
#include "opto/opaquenode.hpp"
#include "opto/superword.hpp"
#include "opto/vectornode.hpp"

//
//                  S U P E R W O R D   T R A N S F O R M
//=============================================================================

//------------------------------SuperWord---------------------------
SuperWord::SuperWord(PhaseIdealLoop* phase) :
  _phase(phase),
  _igvn(phase->_igvn),
  _arena(phase->C->comp_arena()),
  _packset(arena(), 8,  0, NULL),         // packs for the current block
  _bb_idx(arena(), (int)(1.10 * phase->C->unique()), 0, 0), // node idx to index in bb
  _block(arena(), 8,  0, NULL),           // nodes in current block
  _data_entry(arena(), 8,  0, NULL),      // nodes with all inputs from outside
  _mem_slice_head(arena(), 8,  0, NULL),  // memory slice heads
  _mem_slice_tail(arena(), 8,  0, NULL),  // memory slice tails
  _node_info(arena(), 8,  0, SWNodeInfo::initial), // info needed per node
  _clone_map(phase->C->clone_map()),      // map of nodes created in cloning
  _align_to_ref(NULL),                    // memory reference to align vectors to
  _disjoint_ptrs(arena(), 8,  0, OrderedPair::initial), // runtime disambiguated pointer pairs
  _dg(_arena),                            // dependence graph
  _visited(arena()),                      // visited node set
  _post_visited(arena()),                 // post visited node set
  _n_idx_list(arena(), 8),                // scratch list of (node,index) pairs
  _stk(arena(), 8, 0, NULL),              // scratch stack of nodes
  _nlist(arena(), 8, 0, NULL),            // scratch list of nodes
  _lpt(NULL),                             // loop tree node
  _lp(NULL),                              // LoopNode
  _bb(NULL),                              // basic block
  _iv(NULL),                              // induction var
  _race_possible(false),                  // cases where SDMU is true
  _num_work_vecs(0),                      // amount of vector work we have
  _num_reductions(0),                     // amount of reduction work we have
  _do_vector_loop(phase->C->do_vector_loop()),  // whether to do vectorization/simd style
  _ii_first(-1),                          // first loop generation index - only if do_vector_loop()
  _ii_last(-1),                           // last loop generation index - only if do_vector_loop()
  _ii_order(arena(), 8, 0, 0),
  _vector_loop_debug(phase->C->has_method() && phase->C->method_has_option("VectorizeDebug"))
{}

//------------------------------transform_loop---------------------------
void SuperWord::transform_loop(IdealLoopTree* lpt) {
  assert(UseSuperWord, "should be");
  // Do vectors exist on this architecture?
  if (Matcher::vector_width_in_bytes(T_BYTE) < 2) return;

  assert(lpt->_head->is_CountedLoop(), "must be");
  CountedLoopNode *cl = lpt->_head->as_CountedLoop();

  if (!cl->is_valid_counted_loop()) return; // skip malformed counted loop

  if (!cl->is_main_loop() ) return; // skip normal, pre, and post loops

  // Check for no control flow in body (other than exit)
  Node *cl_exit = cl->loopexit();
  if (cl_exit->in(0) != lpt->_head) return;

  // Make sure the are no extra control users of the loop backedge
  if (cl->back_control()->outcnt() != 1) {
    return;
  }

  // Check for pre-loop ending with CountedLoopEnd(Bool(Cmp(x,Opaque1(limit))))
  CountedLoopEndNode* pre_end = get_pre_loop_end(cl);
  if (pre_end == NULL) return;
  Node *pre_opaq1 = pre_end->limit();
  if (pre_opaq1->Opcode() != Op_Opaque1) return;

  init(); // initialize data structures

  set_lpt(lpt);
  set_lp(cl);

  // For now, define one block which is the entire loop body
  set_bb(cl);

  assert(_packset.length() == 0, "packset must be empty");
  SLP_extract();
}

//------------------------------SLP_extract---------------------------
// Extract the superword level parallelism
//
// 1) A reverse post-order of nodes in the block is constructed.  By scanning
//    this list from first to last, all definitions are visited before their uses.
//
// 2) A point-to-point dependence graph is constructed between memory references.
//    This simplies the upcoming "independence" checker.
//
// 3) The maximum depth in the node graph from the beginning of the block
//    to each node is computed.  This is used to prune the graph search
//    in the independence checker.
//
// 4) For integer types, the necessary bit width is propagated backwards
//    from stores to allow packed operations on byte, char, and short
//    integers.  This reverses the promotion to type "int" that javac
//    did for operations like: char c1,c2,c3;  c1 = c2 + c3.
//
// 5) One of the memory references is picked to be an aligned vector reference.
//    The pre-loop trip count is adjusted to align this reference in the
//    unrolled body.
//
// 6) The initial set of pack pairs is seeded with memory references.
//
// 7) The set of pack pairs is extended by following use->def and def->use links.
//
// 8) The pairs are combined into vector sized packs.
//
// 9) Reorder the memory slices to co-locate members of the memory packs.
//
// 10) Generate ideal vector nodes for the final set of packs and where necessary,
//    inserting scalar promotion, vector creation from multiple scalars, and
//    extraction of scalar values from vectors.
//
void SuperWord::SLP_extract() {

#ifndef PRODUCT
  if (_do_vector_loop && TraceSuperWord) {
    tty->print("SuperWord::SLP_extract\n");
    tty->print("input loop\n");
    _lpt->dump_head();
    _lpt->dump();
    for (uint i = 0; i < _lpt->_body.size(); i++) {
      _lpt->_body.at(i)->dump();
    }
  }
#endif
  // Ready the block
  if (!construct_bb()) {
    return; // Exit if no interesting nodes or complex graph.
  }
  // build    _dg, _disjoint_ptrs
  dependence_graph();

  // compute function depth(Node*)
  compute_max_depth();

  if (_do_vector_loop) {
    if (mark_generations() != -1) {
      hoist_loads_in_graph(); // this only rebuild the graph; all basic structs need rebuild explicitly

      if (!construct_bb()) {
        return; // Exit if no interesting nodes or complex graph.
      }
      dependence_graph();
      compute_max_depth();
    }

#ifndef PRODUCT
    if (TraceSuperWord) {
      tty->print_cr("\nSuperWord::_do_vector_loop: graph after hoist_loads_in_graph");
      _lpt->dump_head();
      for (int j = 0; j < _block.length(); j++) {
        Node* n = _block.at(j);
        int d = depth(n);
        for (int i = 0;  i < d; i++) tty->print("%s", "  ");
        tty->print("%d :", d);
        n->dump();
      }
    }
#endif
  }

  compute_vector_element_type();

  // Attempt vectorization

  find_adjacent_refs();

  extend_packlist();

  if (_do_vector_loop) {
    if (_packset.length() == 0) {
#ifndef PRODUCT
      if (TraceSuperWord) {
        tty->print_cr("\nSuperWord::_do_vector_loop DFA could not build packset, now trying to build anyway");
      }
#endif
      pack_parallel();
    }
  }

  combine_packs();

  construct_my_pack_map();

  filter_packs();

  schedule();

  output();
}

//------------------------------find_adjacent_refs---------------------------
// Find the adjacent memory references and create pack pairs for them.
// This is the initial set of packs that will then be extended by
// following use->def and def->use links.  The align positions are
// assigned relative to the reference "align_to_ref"
void SuperWord::find_adjacent_refs() {
  // Get list of memory operations
  Node_List memops;
  for (int i = 0; i < _block.length(); i++) {
    Node* n = _block.at(i);
    if (n->is_Mem() && !n->is_LoadStore() && in_bb(n) &&
        is_java_primitive(n->as_Mem()->memory_type())) {
      int align = memory_alignment(n->as_Mem(), 0);
      if (align != bottom_align) {
        memops.push(n);
      }
    }
  }

  Node_List align_to_refs;
  int best_iv_adjustment = 0;
  MemNode* best_align_to_mem_ref = NULL;

  while (memops.size() != 0) {
    // Find a memory reference to align to.
    MemNode* mem_ref = find_align_to_ref(memops);
    if (mem_ref == NULL) break;
    align_to_refs.push(mem_ref);
    int iv_adjustment = get_iv_adjustment(mem_ref);

    if (best_align_to_mem_ref == NULL) {
      // Set memory reference which is the best from all memory operations
      // to be used for alignment. The pre-loop trip count is modified to align
      // this reference to a vector-aligned address.
      best_align_to_mem_ref = mem_ref;
      best_iv_adjustment = iv_adjustment;
    }

    SWPointer align_to_ref_p(mem_ref, this);
    // Set alignment relative to "align_to_ref" for all related memory operations.
    for (int i = memops.size() - 1; i >= 0; i--) {
      MemNode* s = memops.at(i)->as_Mem();
      if (isomorphic(s, mem_ref)) {
        SWPointer p2(s, this);
        if (p2.comparable(align_to_ref_p)) {
          int align = memory_alignment(s, iv_adjustment);
          set_alignment(s, align);
        }
      }
    }

    // Create initial pack pairs of memory operations for which
    // alignment is set and vectors will be aligned.
    bool create_pack = true;
    if (memory_alignment(mem_ref, best_iv_adjustment) == 0 || _do_vector_loop) {
      if (!Matcher::misaligned_vectors_ok()) {
        int vw = vector_width(mem_ref);
        int vw_best = vector_width(best_align_to_mem_ref);
        if (vw > vw_best) {
          // Do not vectorize a memory access with more elements per vector
          // if unaligned memory access is not allowed because number of
          // iterations in pre-loop will be not enough to align it.
          create_pack = false;
        }
      }
    } else {
      if (same_velt_type(mem_ref, best_align_to_mem_ref)) {
        // Can't allow vectorization of unaligned memory accesses with the
        // same type since it could be overlapped accesses to the same array.
        create_pack = false;
      } else {
        // Allow independent (different type) unaligned memory operations
        // if HW supports them.
        if (!Matcher::misaligned_vectors_ok()) {
          create_pack = false;
        } else {
          // Check if packs of the same memory type but
          // with a different alignment were created before.
          for (uint i = 0; i < align_to_refs.size(); i++) {
            MemNode* mr = align_to_refs.at(i)->as_Mem();
            if (same_velt_type(mr, mem_ref) &&
                memory_alignment(mr, iv_adjustment) != 0)
              create_pack = false;
          }
        }
      }
    }
    if (create_pack) {
      for (uint i = 0; i < memops.size(); i++) {
        Node* s1 = memops.at(i);
        int align = alignment(s1);
        if (align == top_align) continue;
        for (uint j = 0; j < memops.size(); j++) {
          Node* s2 = memops.at(j);
          if (alignment(s2) == top_align) continue;
          if (s1 != s2 && are_adjacent_refs(s1, s2)) {
            if (stmts_can_pack(s1, s2, align)) {
              Node_List* pair = new Node_List();
              pair->push(s1);
              pair->push(s2);
              if (!_do_vector_loop || _clone_map.idx(s1->_idx) == _clone_map.idx(s2->_idx)) {
                _packset.append(pair);
              }
            }
          }
        }
      }
    } else { // Don't create unaligned pack
      // First, remove remaining memory ops of the same type from the list.
      for (int i = memops.size() - 1; i >= 0; i--) {
        MemNode* s = memops.at(i)->as_Mem();
        if (same_velt_type(s, mem_ref)) {
          memops.remove(i);
        }
      }

      // Second, remove already constructed packs of the same type.
      for (int i = _packset.length() - 1; i >= 0; i--) {
        Node_List* p = _packset.at(i);
        MemNode* s = p->at(0)->as_Mem();
        if (same_velt_type(s, mem_ref)) {
          remove_pack_at(i);
        }
      }

      // If needed find the best memory reference for loop alignment again.
      if (same_velt_type(mem_ref, best_align_to_mem_ref)) {
        // Put memory ops from remaining packs back on memops list for
        // the best alignment search.
        uint orig_msize = memops.size();
        for (int i = 0; i < _packset.length(); i++) {
          Node_List* p = _packset.at(i);
          MemNode* s = p->at(0)->as_Mem();
          assert(!same_velt_type(s, mem_ref), "sanity");
          memops.push(s);
        }
        MemNode* best_align_to_mem_ref = find_align_to_ref(memops);
        if (best_align_to_mem_ref == NULL) break;
        best_iv_adjustment = get_iv_adjustment(best_align_to_mem_ref);
        // Restore list.
        while (memops.size() > orig_msize)
          (void)memops.pop();
      }
    } // unaligned memory accesses

    // Remove used mem nodes.
    for (int i = memops.size() - 1; i >= 0; i--) {
      MemNode* m = memops.at(i)->as_Mem();
      if (alignment(m) != top_align) {
        memops.remove(i);
      }
    }

  } // while (memops.size() != 0
  set_align_to_ref(best_align_to_mem_ref);

#ifndef PRODUCT
  if (TraceSuperWord) {
    tty->print_cr("\nAfter find_adjacent_refs");
    print_packset();
  }
#endif
}

//------------------------------find_align_to_ref---------------------------
// Find a memory reference to align the loop induction variable to.
// Looks first at stores then at loads, looking for a memory reference
// with the largest number of references similar to it.
MemNode* SuperWord::find_align_to_ref(Node_List &memops) {
  GrowableArray<int> cmp_ct(arena(), memops.size(), memops.size(), 0);

  // Count number of comparable memory ops
  for (uint i = 0; i < memops.size(); i++) {
    MemNode* s1 = memops.at(i)->as_Mem();
    SWPointer p1(s1, this);
    // Discard if pre loop can't align this reference
    if (!ref_is_alignable(p1)) {
      *cmp_ct.adr_at(i) = 0;
      continue;
    }
    for (uint j = i+1; j < memops.size(); j++) {
      MemNode* s2 = memops.at(j)->as_Mem();
      if (isomorphic(s1, s2)) {
        SWPointer p2(s2, this);
        if (p1.comparable(p2)) {
          (*cmp_ct.adr_at(i))++;
          (*cmp_ct.adr_at(j))++;
        }
      }
    }
  }

  // Find Store (or Load) with the greatest number of "comparable" references,
  // biggest vector size, smallest data size and smallest iv offset.
  int max_ct        = 0;
  int max_vw        = 0;
  int max_idx       = -1;
  int min_size      = max_jint;
  int min_iv_offset = max_jint;
  for (uint j = 0; j < memops.size(); j++) {
    MemNode* s = memops.at(j)->as_Mem();
    if (s->is_Store()) {
      int vw = vector_width_in_bytes(s);
      assert(vw > 1, "sanity");
      SWPointer p(s, this);
      if (cmp_ct.at(j) >  max_ct ||
          cmp_ct.at(j) == max_ct &&
            (vw >  max_vw ||
             vw == max_vw &&
              (data_size(s) <  min_size ||
               data_size(s) == min_size &&
                 (p.offset_in_bytes() < min_iv_offset)))) {
        max_ct = cmp_ct.at(j);
        max_vw = vw;
        max_idx = j;
        min_size = data_size(s);
        min_iv_offset = p.offset_in_bytes();
      }
    }
  }
  // If no stores, look at loads
  if (max_ct == 0) {
    for (uint j = 0; j < memops.size(); j++) {
      MemNode* s = memops.at(j)->as_Mem();
      if (s->is_Load()) {
        int vw = vector_width_in_bytes(s);
        assert(vw > 1, "sanity");
        SWPointer p(s, this);
        if (cmp_ct.at(j) >  max_ct ||
            cmp_ct.at(j) == max_ct &&
              (vw >  max_vw ||
               vw == max_vw &&
                (data_size(s) <  min_size ||
                 data_size(s) == min_size &&
                   (p.offset_in_bytes() < min_iv_offset)))) {
          max_ct = cmp_ct.at(j);
          max_vw = vw;
          max_idx = j;
          min_size = data_size(s);
          min_iv_offset = p.offset_in_bytes();
        }
      }
    }
  }

#ifdef ASSERT
  if (TraceSuperWord && Verbose) {
    tty->print_cr("\nVector memops after find_align_to_ref");
    for (uint i = 0; i < memops.size(); i++) {
      MemNode* s = memops.at(i)->as_Mem();
      s->dump();
    }
  }
#endif

  if (max_ct > 0) {
#ifdef ASSERT
    if (TraceSuperWord) {
      tty->print("\nVector align to node: ");
      memops.at(max_idx)->as_Mem()->dump();
    }
#endif
    return memops.at(max_idx)->as_Mem();
  }
  return NULL;
}

//------------------------------ref_is_alignable---------------------------
// Can the preloop align the reference to position zero in the vector?
bool SuperWord::ref_is_alignable(SWPointer& p) {
  if (!p.has_iv()) {
    return true;   // no induction variable
  }
  CountedLoopEndNode* pre_end = get_pre_loop_end(lp()->as_CountedLoop());
  assert(pre_end != NULL, "we must have a correct pre-loop");
  assert(pre_end->stride_is_con(), "pre loop stride is constant");
  int preloop_stride = pre_end->stride_con();

  int span = preloop_stride * p.scale_in_bytes();
  int mem_size = p.memory_size();
  int offset   = p.offset_in_bytes();
  // Stride one accesses are alignable if offset is aligned to memory operation size.
  // Offset can be unaligned when UseUnalignedAccesses is used.
  if (ABS(span) == mem_size && (ABS(offset) % mem_size) == 0) {
    return true;
  }
  // If initial offset from start of object is computable,
  // compute alignment within the vector.
  int vw = vector_width_in_bytes(p.mem());
  assert(vw > 1, "sanity");
  if (vw % span == 0) {
    Node* init_nd = pre_end->init_trip();
    if (init_nd->is_Con() && p.invar() == NULL) {
      int init = init_nd->bottom_type()->is_int()->get_con();

      int init_offset = init * p.scale_in_bytes() + offset;
      assert(init_offset >= 0, "positive offset from object start");

      if (span > 0) {
        return (vw - (init_offset % vw)) % span == 0;
      } else {
        assert(span < 0, "nonzero stride * scale");
        return (init_offset % vw) % -span == 0;
      }
    }
  }
  return false;
}

//---------------------------get_iv_adjustment---------------------------
// Calculate loop's iv adjustment for this memory ops.
int SuperWord::get_iv_adjustment(MemNode* mem_ref) {
  SWPointer align_to_ref_p(mem_ref, this);
  int offset = align_to_ref_p.offset_in_bytes();
  int scale  = align_to_ref_p.scale_in_bytes();
  int vw       = vector_width_in_bytes(mem_ref);
  assert(vw > 1, "sanity");
  int stride_sign   = (scale * iv_stride()) > 0 ? 1 : -1;
  // At least one iteration is executed in pre-loop by default. As result
  // several iterations are needed to align memory operations in main-loop even
  // if offset is 0.
  int iv_adjustment_in_bytes = (stride_sign * vw - (offset % vw));
  int elt_size = align_to_ref_p.memory_size();
  assert(((ABS(iv_adjustment_in_bytes) % elt_size) == 0),
         err_msg_res("(%d) should be divisible by (%d)", iv_adjustment_in_bytes, elt_size));
  int iv_adjustment = iv_adjustment_in_bytes/elt_size;

#ifndef PRODUCT
  if (TraceSuperWord)
    tty->print_cr("\noffset = %d iv_adjust = %d elt_size = %d scale = %d iv_stride = %d vect_size %d",
                  offset, iv_adjustment, elt_size, scale, iv_stride(), vw);
#endif
  return iv_adjustment;
}

//---------------------------dependence_graph---------------------------
// Construct dependency graph.
// Add dependence edges to load/store nodes for memory dependence
//    A.out()->DependNode.in(1) and DependNode.out()->B.prec(x)
void SuperWord::dependence_graph() {
  // First, assign a dependence node to each memory node
  for (int i = 0; i < _block.length(); i++ ) {
    Node *n = _block.at(i);
    if (n->is_Mem() || n->is_Phi() && n->bottom_type() == Type::MEMORY) {
      _dg.make_node(n);
    }
  }

  // For each memory slice, create the dependences
  for (int i = 0; i < _mem_slice_head.length(); i++) {
    Node* n      = _mem_slice_head.at(i);
    Node* n_tail = _mem_slice_tail.at(i);

    // Get slice in predecessor order (last is first)
    mem_slice_preds(n_tail, n, _nlist);

#ifndef PRODUCT
    if(TraceSuperWord && Verbose) {
      tty->print_cr("SuperWord::dependence_graph: built a new mem slice");
      for (int j = _nlist.length() - 1; j >= 0 ; j--) {
        _nlist.at(j)->dump();
      }
    }
#endif
    // Make the slice dependent on the root
    DepMem* slice = _dg.dep(n);
    _dg.make_edge(_dg.root(), slice);

    // Create a sink for the slice
    DepMem* slice_sink = _dg.make_node(NULL);
    _dg.make_edge(slice_sink, _dg.tail());

    // Now visit each pair of memory ops, creating the edges
    for (int j = _nlist.length() - 1; j >= 0 ; j--) {
      Node* s1 = _nlist.at(j);

      // If no dependency yet, use slice
      if (_dg.dep(s1)->in_cnt() == 0) {
        _dg.make_edge(slice, s1);
      }
      SWPointer p1(s1->as_Mem(), this);
      bool sink_dependent = true;
      for (int k = j - 1; k >= 0; k--) {
        Node* s2 = _nlist.at(k);
        if (s1->is_Load() && s2->is_Load())
          continue;
        SWPointer p2(s2->as_Mem(), this);

        int cmp = p1.cmp(p2);
        if (SuperWordRTDepCheck &&
            p1.base() != p2.base() && p1.valid() && p2.valid()) {
          // Create a runtime check to disambiguate
          OrderedPair pp(p1.base(), p2.base());
          _disjoint_ptrs.append_if_missing(pp);
        } else if (!SWPointer::not_equal(cmp)) {
          // Possibly same address
          _dg.make_edge(s1, s2);
          sink_dependent = false;
        }
      }
      if (sink_dependent) {
        _dg.make_edge(s1, slice_sink);
      }
    }
#ifndef PRODUCT
    if (TraceSuperWord) {
      tty->print_cr("\nDependence graph for slice: %d", n->_idx);
      for (int q = 0; q < _nlist.length(); q++) {
        _dg.print(_nlist.at(q));
      }
      tty->cr();
    }
#endif
    _nlist.clear();
  }

#ifndef PRODUCT
  if (TraceSuperWord) {
    tty->print_cr("\ndisjoint_ptrs: %s", _disjoint_ptrs.length() > 0 ? "" : "NONE");
    for (int r = 0; r < _disjoint_ptrs.length(); r++) {
      _disjoint_ptrs.at(r).print();
      tty->cr();
    }
    tty->cr();
  }
#endif
}

//---------------------------mem_slice_preds---------------------------
// Return a memory slice (node list) in predecessor order starting at "start"
void SuperWord::mem_slice_preds(Node* start, Node* stop, GrowableArray<Node*> &preds) {
  assert(preds.length() == 0, "start empty");
  Node* n = start;
  Node* prev = NULL;
  while (true) {
    assert(in_bb(n), "must be in block");
    for (DUIterator_Fast imax, i = n->fast_outs(imax); i < imax; i++) {
      Node* out = n->fast_out(i);
      if (out->is_Load()) {
        if (in_bb(out)) {
          preds.push(out);
        }
      } else {
        // FIXME
        if (out->is_MergeMem() && !in_bb(out)) {
          // Either unrolling is causing a memory edge not to disappear,
          // or need to run igvn.optimize() again before SLP
        } else if (out->is_Phi() && out->bottom_type() == Type::MEMORY && !in_bb(out)) {
          // Ditto.  Not sure what else to check further.
        } else if (out->Opcode() == Op_StoreCM && out->in(MemNode::OopStore) == n) {
          // StoreCM has an input edge used as a precedence edge.
          // Maybe an issue when oop stores are vectorized.
        } else {
          assert(out == prev || prev == NULL, "no branches off of store slice");
        }
      }
    }
    if (n == stop) break;
    preds.push(n);
    prev = n;
    assert(n->is_Mem(), err_msg_res("unexpected node %s", n->Name()));
    n = n->in(MemNode::Memory);
  }
}

//------------------------------stmts_can_pack---------------------------
// Can s1 and s2 be in a pack with s1 immediately preceding s2 and
// s1 aligned at "align"
bool SuperWord::stmts_can_pack(Node* s1, Node* s2, int align) {

  // Do not use superword for non-primitives
  BasicType bt1 = velt_basic_type(s1);
  BasicType bt2 = velt_basic_type(s2);
  if(!is_java_primitive(bt1) || !is_java_primitive(bt2))
    return false;
  if (Matcher::max_vector_size(bt1) < 2) {
    return false; // No vectors for this type
  }

  if (isomorphic(s1, s2)) {
    if (independent(s1, s2) || reduction(s1, s2)) {
      if (!exists_at(s1, 0) && !exists_at(s2, 1)) {
        if (!s1->is_Mem() || are_adjacent_refs(s1, s2)) {
          int s1_align = alignment(s1);
          int s2_align = alignment(s2);
          if (s1_align == top_align || s1_align == align) {
            if (s2_align == top_align || s2_align == align + data_size(s1)) {
              return true;
            }
          }
        }
      }
    }
  }
  return false;
}

//------------------------------exists_at---------------------------
// Does s exist in a pack at position pos?
bool SuperWord::exists_at(Node* s, uint pos) {
  for (int i = 0; i < _packset.length(); i++) {
    Node_List* p = _packset.at(i);
    if (p->at(pos) == s) {
      return true;
    }
  }
  return false;
}

//------------------------------are_adjacent_refs---------------------------
// Is s1 immediately before s2 in memory?
bool SuperWord::are_adjacent_refs(Node* s1, Node* s2) {
  if (!s1->is_Mem() || !s2->is_Mem()) return false;
  if (!in_bb(s1)    || !in_bb(s2))    return false;

  // Do not use superword for non-primitives
  if (!is_java_primitive(s1->as_Mem()->memory_type()) ||
      !is_java_primitive(s2->as_Mem()->memory_type())) {
    return false;
  }

  // FIXME - co_locate_pack fails on Stores in different mem-slices, so
  // only pack memops that are in the same alias set until that's fixed.
  if (_phase->C->get_alias_index(s1->as_Mem()->adr_type()) !=
      _phase->C->get_alias_index(s2->as_Mem()->adr_type()))
    return false;
  SWPointer p1(s1->as_Mem(), this);
  SWPointer p2(s2->as_Mem(), this);
  if (p1.base() != p2.base() || !p1.comparable(p2)) return false;
  int diff = p2.offset_in_bytes() - p1.offset_in_bytes();
  return diff == data_size(s1);
}

//------------------------------isomorphic---------------------------
// Are s1 and s2 similar?
bool SuperWord::isomorphic(Node* s1, Node* s2) {
  if (s1->Opcode() != s2->Opcode()) return false;
  if (s1->req() != s2->req()) return false;
  if (s1->in(0) != s2->in(0)) return false;
  if (!same_velt_type(s1, s2)) return false;
  return true;
}

//------------------------------independent---------------------------
// Is there no data path from s1 to s2 or s2 to s1?
bool SuperWord::independent(Node* s1, Node* s2) {
  //  assert(s1->Opcode() == s2->Opcode(), "check isomorphic first");
  int d1 = depth(s1);
  int d2 = depth(s2);
  if (d1 == d2) return s1 != s2;
  Node* deep    = d1 > d2 ? s1 : s2;
  Node* shallow = d1 > d2 ? s2 : s1;

  visited_clear();

  return independent_path(shallow, deep);
}

//------------------------------reduction---------------------------
// Is there a data path between s1 and s2 and the nodes reductions?
bool SuperWord::reduction(Node* s1, Node* s2) {
  bool retValue = false;
  int d1 = depth(s1);
  int d2 = depth(s2);
  if (d1 + 1 == d2) {
    if (s1->is_reduction() && s2->is_reduction()) {
      // This is an ordered set, so s1 should define s2
      for (DUIterator_Fast imax, i = s1->fast_outs(imax); i < imax; i++) {
        Node* t1 = s1->fast_out(i);
        if (t1 == s2) {
          // both nodes are reductions and connected
          retValue = true;
        }
      }
    }
  }

  return retValue;
}

//------------------------------independent_path------------------------------
// Helper for independent
bool SuperWord::independent_path(Node* shallow, Node* deep, uint dp) {
  if (dp >= 1000) return false; // stop deep recursion
  visited_set(deep);
  int shal_depth = depth(shallow);
  assert(shal_depth <= depth(deep), "must be");
  for (DepPreds preds(deep, _dg); !preds.done(); preds.next()) {
    Node* pred = preds.current();
    if (in_bb(pred) && !visited_test(pred)) {
      if (shallow == pred) {
        return false;
      }
      if (shal_depth < depth(pred) && !independent_path(shallow, pred, dp+1)) {
        return false;
      }
    }
  }
  return true;
}

//------------------------------set_alignment---------------------------
void SuperWord::set_alignment(Node* s1, Node* s2, int align) {
  set_alignment(s1, align);
  if (align == top_align || align == bottom_align) {
    set_alignment(s2, align);
  } else {
    set_alignment(s2, align + data_size(s1));
  }
}

//------------------------------data_size---------------------------
int SuperWord::data_size(Node* s) {
  int bsize = type2aelembytes(velt_basic_type(s));
  assert(bsize != 0, "valid size");
  return bsize;
}

//------------------------------extend_packlist---------------------------
// Extend packset by following use->def and def->use links from pack members.
void SuperWord::extend_packlist() {
  bool changed;
  do {
    packset_sort(_packset.length());
    changed = false;
    for (int i = 0; i < _packset.length(); i++) {
      Node_List* p = _packset.at(i);
      changed |= follow_use_defs(p);
      changed |= follow_def_uses(p);
    }
  } while (changed);

  if (_race_possible) {
    for (int i = 0; i < _packset.length(); i++) {
      Node_List* p = _packset.at(i);
      order_def_uses(p);
    }
  }

#ifndef PRODUCT
  if (TraceSuperWord) {
    tty->print_cr("\nAfter extend_packlist");
    print_packset();
  }
#endif
}

//------------------------------follow_use_defs---------------------------
// Extend the packset by visiting operand definitions of nodes in pack p
bool SuperWord::follow_use_defs(Node_List* p) {
  assert(p->size() == 2, "just checking");
  Node* s1 = p->at(0);
  Node* s2 = p->at(1);
  assert(s1->req() == s2->req(), "just checking");
  assert(alignment(s1) + data_size(s1) == alignment(s2), "just checking");

  if (s1->is_Load()) return false;

  int align = alignment(s1);
  bool changed = false;
  int start = s1->is_Store() ? MemNode::ValueIn   : 1;
  int end   = s1->is_Store() ? MemNode::ValueIn+1 : s1->req();
  for (int j = start; j < end; j++) {
    Node* t1 = s1->in(j);
    Node* t2 = s2->in(j);
    if (!in_bb(t1) || !in_bb(t2))
      continue;
    if (stmts_can_pack(t1, t2, align)) {
      if (est_savings(t1, t2) >= 0) {
        Node_List* pair = new Node_List();
        pair->push(t1);
        pair->push(t2);
        _packset.append(pair);
        set_alignment(t1, t2, align);
        changed = true;
      }
    }
  }
  return changed;
}

//------------------------------follow_def_uses---------------------------
// Extend the packset by visiting uses of nodes in pack p
bool SuperWord::follow_def_uses(Node_List* p) {
  bool changed = false;
  Node* s1 = p->at(0);
  Node* s2 = p->at(1);
  assert(p->size() == 2, "just checking");
  assert(s1->req() == s2->req(), "just checking");
  assert(alignment(s1) + data_size(s1) == alignment(s2), "just checking");

  if (s1->is_Store()) return false;

  int align = alignment(s1);
  int savings = -1;
  int num_s1_uses = 0;
  Node* u1 = NULL;
  Node* u2 = NULL;
  for (DUIterator_Fast imax, i = s1->fast_outs(imax); i < imax; i++) {
    Node* t1 = s1->fast_out(i);
    num_s1_uses++;
    if (!in_bb(t1)) continue;
    for (DUIterator_Fast jmax, j = s2->fast_outs(jmax); j < jmax; j++) {
      Node* t2 = s2->fast_out(j);
      if (!in_bb(t2)) continue;
      if (!opnd_positions_match(s1, t1, s2, t2))
        continue;
      if (stmts_can_pack(t1, t2, align)) {
        int my_savings = est_savings(t1, t2);
        if (my_savings > savings) {
          savings = my_savings;
          u1 = t1;
          u2 = t2;
        }
      }
    }
  }
  if (num_s1_uses > 1) {
    _race_possible = true;
  }
  if (savings >= 0) {
    Node_List* pair = new Node_List();
    pair->push(u1);
    pair->push(u2);
    _packset.append(pair);
    set_alignment(u1, u2, align);
    changed = true;
  }
  return changed;
}

//------------------------------order_def_uses---------------------------
// For extended packsets, ordinally arrange uses packset by major component
void SuperWord::order_def_uses(Node_List* p) {
  Node* s1 = p->at(0);

  if (s1->is_Store()) return;

  // reductions are always managed beforehand
  if (s1->is_reduction()) return;

  for (DUIterator_Fast imax, i = s1->fast_outs(imax); i < imax; i++) {
    Node* t1 = s1->fast_out(i);

    // Only allow operand swap on commuting operations
    if (!t1->is_Add() && !t1->is_Mul()) {
      break;
    }

    // Now find t1's packset
    Node_List* p2 = NULL;
    for (int j = 0; j < _packset.length(); j++) {
      p2 = _packset.at(j);
      Node* first = p2->at(0);
      if (t1 == first) {
        break;
      }
      p2 = NULL;
    }
    // Arrange all sub components by the major component
    if (p2 != NULL) {
      for (uint j = 1; j < p->size(); j++) {
        Node* d1 = p->at(j);
        Node* u1 = p2->at(j);
        opnd_positions_match(s1, t1, d1, u1);
      }
    }
  }
}

//---------------------------opnd_positions_match-------------------------
// Is the use of d1 in u1 at the same operand position as d2 in u2?
bool SuperWord::opnd_positions_match(Node* d1, Node* u1, Node* d2, Node* u2) {
  // check reductions to see if they are marshalled to represent the reduction
  // operator in a specified opnd
  if (u1->is_reduction() && u2->is_reduction()) {
    // ensure reductions have phis and reduction definitions feeding the 1st operand
    Node* first = u1->in(2);
    if (first->is_Phi() || first->is_reduction()) {
      u1->swap_edges(1, 2);
    }
    // ensure reductions have phis and reduction definitions feeding the 1st operand
    first = u2->in(2);
    if (first->is_Phi() || first->is_reduction()) {
      u2->swap_edges(1, 2);
    }
    return true;
  }

  uint ct = u1->req();
  if (ct != u2->req()) return false;
  uint i1 = 0;
  uint i2 = 0;
  do {
    for (i1++; i1 < ct; i1++) if (u1->in(i1) == d1) break;
    for (i2++; i2 < ct; i2++) if (u2->in(i2) == d2) break;
    if (i1 != i2) {
      if ((i1 == (3-i2)) && (u2->is_Add() || u2->is_Mul())) {
        // Further analysis relies on operands position matching.
        u2->swap_edges(i1, i2);
      } else {
        return false;
      }
    }
  } while (i1 < ct);
  return true;
}

//------------------------------est_savings---------------------------
// Estimate the savings from executing s1 and s2 as a pack
int SuperWord::est_savings(Node* s1, Node* s2) {
  int save_in = 2 - 1; // 2 operations per instruction in packed form

  // inputs
  for (uint i = 1; i < s1->req(); i++) {
    Node* x1 = s1->in(i);
    Node* x2 = s2->in(i);
    if (x1 != x2) {
      if (are_adjacent_refs(x1, x2)) {
        save_in += adjacent_profit(x1, x2);
      } else if (!in_packset(x1, x2)) {
        save_in -= pack_cost(2);
      } else {
        save_in += unpack_cost(2);
      }
    }
  }

  // uses of result
  uint ct = 0;
  int save_use = 0;
  for (DUIterator_Fast imax, i = s1->fast_outs(imax); i < imax; i++) {
    Node* s1_use = s1->fast_out(i);
    for (int j = 0; j < _packset.length(); j++) {
      Node_List* p = _packset.at(j);
      if (p->at(0) == s1_use) {
        for (DUIterator_Fast kmax, k = s2->fast_outs(kmax); k < kmax; k++) {
          Node* s2_use = s2->fast_out(k);
          if (p->at(p->size()-1) == s2_use) {
            ct++;
            if (are_adjacent_refs(s1_use, s2_use)) {
              save_use += adjacent_profit(s1_use, s2_use);
            }
          }
        }
      }
    }
  }

  if (ct < s1->outcnt()) save_use += unpack_cost(1);
  if (ct < s2->outcnt()) save_use += unpack_cost(1);

  return MAX2(save_in, save_use);
}

//------------------------------costs---------------------------
int SuperWord::adjacent_profit(Node* s1, Node* s2) { return 2; }
int SuperWord::pack_cost(int ct)   { return ct; }
int SuperWord::unpack_cost(int ct) { return ct; }

//------------------------------combine_packs---------------------------
// Combine packs A and B with A.last == B.first into A.first..,A.last,B.second,..B.last
void SuperWord::combine_packs() {
  bool changed = true;
  // Combine packs regardless max vector size.
  while (changed) {
    changed = false;
    for (int i = 0; i < _packset.length(); i++) {
      Node_List* p1 = _packset.at(i);
      if (p1 == NULL) continue;
      // Because of sorting we can start at i + 1
      for (int j = i + 1; j < _packset.length(); j++) {
        Node_List* p2 = _packset.at(j);
        if (p2 == NULL) continue;
        if (i == j) continue;
        if (p1->at(p1->size()-1) == p2->at(0)) {
          for (uint k = 1; k < p2->size(); k++) {
            p1->push(p2->at(k));
          }
          _packset.at_put(j, NULL);
          changed = true;
        }
      }
    }
  }

  // Split packs which have size greater then max vector size.
  for (int i = 0; i < _packset.length(); i++) {
    Node_List* p1 = _packset.at(i);
    if (p1 != NULL) {
      BasicType bt = velt_basic_type(p1->at(0));
      uint max_vlen = Matcher::max_vector_size(bt); // Max elements in vector
      assert(is_power_of_2(max_vlen), "sanity");
      uint psize = p1->size();
      if (!is_power_of_2(psize)) {
        // Skip pack which can't be vector.
        // case1: for(...) { a[i] = i; }    elements values are different (i+x)
        // case2: for(...) { a[i] = b[i+1]; }  can't align both, load and store
        _packset.at_put(i, NULL);
        continue;
      }
      if (psize > max_vlen) {
        Node_List* pack = new Node_List();
        for (uint j = 0; j < psize; j++) {
          pack->push(p1->at(j));
          if (pack->size() >= max_vlen) {
            assert(is_power_of_2(pack->size()), "sanity");
            _packset.append(pack);
            pack = new Node_List();
          }
        }
        _packset.at_put(i, NULL);
      }
    }
  }

  // Compress list.
  for (int i = _packset.length() - 1; i >= 0; i--) {
    Node_List* p1 = _packset.at(i);
    if (p1 == NULL) {
      _packset.remove_at(i);
    }
  }

#ifndef PRODUCT
  if (TraceSuperWord) {
    tty->print_cr("\nAfter combine_packs");
    print_packset();
  }
#endif
}

//-----------------------------construct_my_pack_map--------------------------
// Construct the map from nodes to packs.  Only valid after the
// point where a node is only in one pack (after combine_packs).
void SuperWord::construct_my_pack_map() {
  Node_List* rslt = NULL;
  for (int i = 0; i < _packset.length(); i++) {
    Node_List* p = _packset.at(i);
    for (uint j = 0; j < p->size(); j++) {
      Node* s = p->at(j);
      assert(my_pack(s) == NULL, "only in one pack");
      set_my_pack(s, p);
    }
  }
}

//------------------------------filter_packs---------------------------
// Remove packs that are not implemented or not profitable.
void SuperWord::filter_packs() {
  // Remove packs that are not implemented
  for (int i = _packset.length() - 1; i >= 0; i--) {
    Node_List* pk = _packset.at(i);
    bool impl = implemented(pk);
    if (!impl) {
#ifndef PRODUCT
      if (TraceSuperWord && Verbose) {
        tty->print_cr("Unimplemented");
        pk->at(0)->dump();
      }
#endif
      remove_pack_at(i);
    }
    Node *n = pk->at(0);
    if (n->is_reduction()) {
      _num_reductions++;
    } else {
      _num_work_vecs++;
    }
  }

  // Remove packs that are not profitable
  bool changed;
  do {
    changed = false;
    for (int i = _packset.length() - 1; i >= 0; i--) {
      Node_List* pk = _packset.at(i);
      bool prof = profitable(pk);
      if (!prof) {
#ifndef PRODUCT
        if (TraceSuperWord && Verbose) {
          tty->print_cr("Unprofitable");
          pk->at(0)->dump();
        }
#endif
        remove_pack_at(i);
        changed = true;
      }
    }
  } while (changed);

#ifndef PRODUCT
  if (TraceSuperWord) {
    tty->print_cr("\nAfter filter_packs");
    print_packset();
    tty->cr();
  }
#endif
}

//------------------------------implemented---------------------------
// Can code be generated for pack p?
bool SuperWord::implemented(Node_List* p) {
  bool retValue = false;
  Node* p0 = p->at(0);
  if (p0 != NULL) {
    int opc = p0->Opcode();
    uint size = p->size();
    if (p0->is_reduction()) {
      const Type *arith_type = p0->bottom_type();
      // Length 2 reductions of INT/LONG do not offer performance benefits
      if (((arith_type->basic_type() == T_INT) || (arith_type->basic_type() == T_LONG)) && (size == 2)) {
        retValue = false;
      } else {
        retValue = ReductionNode::implemented(opc, size, arith_type->basic_type());
      }
    } else {
      retValue = VectorNode::implemented(opc, size, velt_basic_type(p0));
    }
  }
  return retValue;
}

//------------------------------same_inputs--------------------------
// For pack p, are all idx operands the same?
static bool same_inputs(Node_List* p, int idx) {
  Node* p0 = p->at(0);
  uint vlen = p->size();
  Node* p0_def = p0->in(idx);
  for (uint i = 1; i < vlen; i++) {
    Node* pi = p->at(i);
    Node* pi_def = pi->in(idx);
    if (p0_def != pi_def)
      return false;
  }
  return true;
}

//------------------------------profitable---------------------------
// For pack p, are all operands and all uses (with in the block) vector?
bool SuperWord::profitable(Node_List* p) {
  Node* p0 = p->at(0);
  uint start, end;
  VectorNode::vector_operands(p0, &start, &end);

  // Return false if some inputs are not vectors or vectors with different
  // size or alignment.
  // Also, for now, return false if not scalar promotion case when inputs are
  // the same. Later, implement PackNode and allow differing, non-vector inputs
  // (maybe just the ones from outside the block.)
  for (uint i = start; i < end; i++) {
    if (!is_vector_use(p0, i))
      return false;
  }
  // Check if reductions are connected
  if (p0->is_reduction()) {
    Node* second_in = p0->in(2);
    Node_List* second_pk = my_pack(second_in);
    if ((second_pk == NULL) || (_num_work_vecs == _num_reductions)) {
      // Remove reduction flag if no parent pack or if not enough work
      // to cover reduction expansion overhead
      p0->remove_flag(Node::Flag_is_reduction);
      return false;
    } else if (second_pk->size() != p->size()) {
      return false;
    }
  }
  if (VectorNode::is_shift(p0)) {
    // For now, return false if shift count is vector or not scalar promotion
    // case (different shift counts) because it is not supported yet.
    Node* cnt = p0->in(2);
    Node_List* cnt_pk = my_pack(cnt);
    if (cnt_pk != NULL)
      return false;
    if (!same_inputs(p, 2))
      return false;
  }
  if (!p0->is_Store()) {
    // For now, return false if not all uses are vector.
    // Later, implement ExtractNode and allow non-vector uses (maybe
    // just the ones outside the block.)
    for (uint i = 0; i < p->size(); i++) {
      Node* def = p->at(i);
      for (DUIterator_Fast jmax, j = def->fast_outs(jmax); j < jmax; j++) {
        Node* use = def->fast_out(j);
        for (uint k = 0; k < use->req(); k++) {
          Node* n = use->in(k);
          if (def == n) {
            // reductions can be loop carried dependences
            if (def->is_reduction() && use->is_Phi())
              continue;
            if (!is_vector_use(use, k)) {
              return false;
            }
          }
        }
      }
    }
  }
  return true;
}

//------------------------------schedule---------------------------
// Adjust the memory graph for the packed operations
void SuperWord::schedule() {

  // Co-locate in the memory graph the members of each memory pack
  for (int i = 0; i < _packset.length(); i++) {
    co_locate_pack(_packset.at(i));
  }
}

//-------------------------------remove_and_insert-------------------
// Remove "current" from its current position in the memory graph and insert
// it after the appropriate insertion point (lip or uip).
void SuperWord::remove_and_insert(MemNode *current, MemNode *prev, MemNode *lip,
                                  Node *uip, Unique_Node_List &sched_before) {
  Node* my_mem = current->in(MemNode::Memory);
  bool sched_up = sched_before.member(current);

  // remove current_store from its current position in the memmory graph
  for (DUIterator i = current->outs(); current->has_out(i); i++) {
    Node* use = current->out(i);
    if (use->is_Mem()) {
      assert(use->in(MemNode::Memory) == current, "must be");
      if (use == prev) { // connect prev to my_mem
          _igvn.replace_input_of(use, MemNode::Memory, my_mem);
          --i; //deleted this edge; rescan position
      } else if (sched_before.member(use)) {
        if (!sched_up) { // Will be moved together with current
          _igvn.replace_input_of(use, MemNode::Memory, uip);
          --i; //deleted this edge; rescan position
        }
      } else {
        if (sched_up) { // Will be moved together with current
          _igvn.replace_input_of(use, MemNode::Memory, lip);
          --i; //deleted this edge; rescan position
        }
      }
    }
  }

  Node *insert_pt =  sched_up ?  uip : lip;

  // all uses of insert_pt's memory state should use current's instead
  for (DUIterator i = insert_pt->outs(); insert_pt->has_out(i); i++) {
    Node* use = insert_pt->out(i);
    if (use->is_Mem()) {
      assert(use->in(MemNode::Memory) == insert_pt, "must be");
      _igvn.replace_input_of(use, MemNode::Memory, current);
      --i; //deleted this edge; rescan position
    } else if (!sched_up && use->is_Phi() && use->bottom_type() == Type::MEMORY) {
      uint pos; //lip (lower insert point) must be the last one in the memory slice
      for (pos=1; pos < use->req(); pos++) {
        if (use->in(pos) == insert_pt) break;
      }
      _igvn.replace_input_of(use, pos, current);
      --i;
    }
  }

  //connect current to insert_pt
  _igvn.replace_input_of(current, MemNode::Memory, insert_pt);
}

//------------------------------co_locate_pack----------------------------------
// To schedule a store pack, we need to move any sandwiched memory ops either before
// or after the pack, based upon dependence information:
// (1) If any store in the pack depends on the sandwiched memory op, the
//     sandwiched memory op must be scheduled BEFORE the pack;
// (2) If a sandwiched memory op depends on any store in the pack, the
//     sandwiched memory op must be scheduled AFTER the pack;
// (3) If a sandwiched memory op (say, memA) depends on another sandwiched
//     memory op (say memB), memB must be scheduled before memA. So, if memA is
//     scheduled before the pack, memB must also be scheduled before the pack;
// (4) If there is no dependence restriction for a sandwiched memory op, we simply
//     schedule this store AFTER the pack
// (5) We know there is no dependence cycle, so there in no other case;
// (6) Finally, all memory ops in another single pack should be moved in the same direction.
//
// To schedule a load pack, we use the memory state of either the first or the last load in
// the pack, based on the dependence constraint.
void SuperWord::co_locate_pack(Node_List* pk) {
  if (pk->at(0)->is_Store()) {
    MemNode* first     = executed_first(pk)->as_Mem();
    MemNode* last      = executed_last(pk)->as_Mem();
    Unique_Node_List schedule_before_pack;
    Unique_Node_List memops;

    MemNode* current   = last->in(MemNode::Memory)->as_Mem();
    MemNode* previous  = last;
    while (true) {
      assert(in_bb(current), "stay in block");
      memops.push(previous);
      for (DUIterator i = current->outs(); current->has_out(i); i++) {
        Node* use = current->out(i);
        if (use->is_Mem() && use != previous)
          memops.push(use);
      }
      if (current == first) break;
      previous = current;
      current  = current->in(MemNode::Memory)->as_Mem();
    }

    // determine which memory operations should be scheduled before the pack
    for (uint i = 1; i < memops.size(); i++) {
      Node *s1 = memops.at(i);
      if (!in_pack(s1, pk) && !schedule_before_pack.member(s1)) {
        for (uint j = 0; j< i; j++) {
          Node *s2 = memops.at(j);
          if (!independent(s1, s2)) {
            if (in_pack(s2, pk) || schedule_before_pack.member(s2)) {
              schedule_before_pack.push(s1); // s1 must be scheduled before
              Node_List* mem_pk = my_pack(s1);
              if (mem_pk != NULL) {
                for (uint ii = 0; ii < mem_pk->size(); ii++) {
                  Node* s = mem_pk->at(ii);  // follow partner
                  if (memops.member(s) && !schedule_before_pack.member(s))
                    schedule_before_pack.push(s);
                }
              }
              break;
            }
          }
        }
      }
    }

    Node*    upper_insert_pt = first->in(MemNode::Memory);
    // Following code moves loads connected to upper_insert_pt below aliased stores.
    // Collect such loads here and reconnect them back to upper_insert_pt later.
    memops.clear();
    for (DUIterator i = upper_insert_pt->outs(); upper_insert_pt->has_out(i); i++) {
      Node* use = upper_insert_pt->out(i);
      if (use->is_Mem() && !use->is_Store()) {
        memops.push(use);
      }
    }

    MemNode* lower_insert_pt = last;
    previous                 = last; //previous store in pk
    current                  = last->in(MemNode::Memory)->as_Mem();

    // start scheduling from "last" to "first"
    while (true) {
      assert(in_bb(current), "stay in block");
      assert(in_pack(previous, pk), "previous stays in pack");
      Node* my_mem = current->in(MemNode::Memory);

      if (in_pack(current, pk)) {
        // Forward users of my memory state (except "previous) to my input memory state
        for (DUIterator i = current->outs(); current->has_out(i); i++) {
          Node* use = current->out(i);
          if (use->is_Mem() && use != previous) {
            assert(use->in(MemNode::Memory) == current, "must be");
            if (schedule_before_pack.member(use)) {
              _igvn.replace_input_of(use, MemNode::Memory, upper_insert_pt);
            } else {
              _igvn.replace_input_of(use, MemNode::Memory, lower_insert_pt);
            }
            --i; // deleted this edge; rescan position
          }
        }
        previous = current;
      } else { // !in_pack(current, pk) ==> a sandwiched store
        remove_and_insert(current, previous, lower_insert_pt, upper_insert_pt, schedule_before_pack);
      }

      if (current == first) break;
      current = my_mem->as_Mem();
    } // end while

    // Reconnect loads back to upper_insert_pt.
    for (uint i = 0; i < memops.size(); i++) {
      Node *ld = memops.at(i);
      if (ld->in(MemNode::Memory) != upper_insert_pt) {
        _igvn.replace_input_of(ld, MemNode::Memory, upper_insert_pt);
      }
    }
  } else if (pk->at(0)->is_Load()) { //load
    // all loads in the pack should have the same memory state. By default,
    // we use the memory state of the last load. However, if any load could
    // not be moved down due to the dependence constraint, we use the memory
    // state of the first load.
    Node* last_mem  = executed_last(pk)->in(MemNode::Memory);
    Node* first_mem = executed_first(pk)->in(MemNode::Memory);
    bool schedule_last = true;
    for (uint i = 0; i < pk->size(); i++) {
      Node* ld = pk->at(i);
      for (Node* current = last_mem; current != ld->in(MemNode::Memory);
           current=current->in(MemNode::Memory)) {
        assert(current != first_mem, "corrupted memory graph");
        if(current->is_Mem() && !independent(current, ld)){
          schedule_last = false; // a later store depends on this load
          break;
        }
      }
    }

    Node* mem_input = schedule_last ? last_mem : first_mem;
    _igvn.hash_delete(mem_input);
    // Give each load the same memory state
    for (uint i = 0; i < pk->size(); i++) {
      LoadNode* ld = pk->at(i)->as_Load();
      _igvn.replace_input_of(ld, MemNode::Memory, mem_input);
    }
  }
}

//------------------------------output---------------------------
// Convert packs into vector node operations
void SuperWord::output() {
  if (_packset.length() == 0) return;

#ifndef PRODUCT
  if (TraceLoopOpts) {
    tty->print("SuperWord    ");
    lpt()->dump_head();
  }
#endif

  // MUST ENSURE main loop's initial value is properly aligned:
  //  (iv_initial_value + min_iv_offset) % vector_width_in_bytes() == 0

  align_initial_loop_index(align_to_ref());

  // Insert extract (unpack) operations for scalar uses
  for (int i = 0; i < _packset.length(); i++) {
    insert_extracts(_packset.at(i));
  }

  Compile* C = _phase->C;
  uint max_vlen_in_bytes = 0;
  for (int i = 0; i < _block.length(); i++) {
    Node* n = _block.at(i);
    Node_List* p = my_pack(n);
    if (p && n == executed_last(p)) {
      uint vlen = p->size();
      uint vlen_in_bytes = 0;
      Node* vn = NULL;
      Node* low_adr = p->at(0);
      Node* first   = executed_first(p);
      int   opc = n->Opcode();
      if (n->is_Load()) {
        Node* ctl = n->in(MemNode::Control);
        Node* mem = first->in(MemNode::Memory);
        SWPointer p1(n->as_Mem(), this);
        // Identify the memory dependency for the new loadVector node by
        // walking up through memory chain.
        // This is done to give flexibility to the new loadVector node so that
        // it can move above independent storeVector nodes.
        while (mem->is_StoreVector()) {
          SWPointer p2(mem->as_Mem(), this);
          int cmp = p1.cmp(p2);
          if (SWPointer::not_equal(cmp) || !SWPointer::comparable(cmp)) {
            mem = mem->in(MemNode::Memory);
          } else {
            break; // dependent memory
          }
        }
        Node* adr = low_adr->in(MemNode::Address);
        const TypePtr* atyp = n->adr_type();
        vn = LoadVectorNode::make(opc, ctl, mem, adr, atyp, vlen, velt_basic_type(n));
        vlen_in_bytes = vn->as_LoadVector()->memory_size();
      } else if (n->is_Store()) {
        // Promote value to be stored to vector
        Node* val = vector_opd(p, MemNode::ValueIn);
        Node* ctl = n->in(MemNode::Control);
        Node* mem = first->in(MemNode::Memory);
        Node* adr = low_adr->in(MemNode::Address);
        const TypePtr* atyp = n->adr_type();
        vn = StoreVectorNode::make(opc, ctl, mem, adr, atyp, val, vlen);
        vlen_in_bytes = vn->as_StoreVector()->memory_size();
      } else if (n->req() == 3) {
        // Promote operands to vector
        Node* in1 = NULL;
        bool node_isa_reduction = n->is_reduction();
        if (node_isa_reduction) {
          // the input to the first reduction operation is retained
          in1 = low_adr->in(1);
        } else {
          in1 = vector_opd(p, 1);
        }
        Node* in2 = vector_opd(p, 2);
        if (VectorNode::is_invariant_vector(in1) && (node_isa_reduction == false) && (n->is_Add() || n->is_Mul())) {
          // Move invariant vector input into second position to avoid register spilling.
          Node* tmp = in1;
          in1 = in2;
          in2 = tmp;
        }
        if (node_isa_reduction) {
          const Type *arith_type = n->bottom_type();
          vn = ReductionNode::make(opc, NULL, in1, in2, arith_type->basic_type());
          if (in2->is_Load()) {
            vlen_in_bytes = in2->as_LoadVector()->memory_size();
          } else {
            vlen_in_bytes = in2->as_Vector()->length_in_bytes();
          }
        } else {
          vn = VectorNode::make(opc, in1, in2, vlen, velt_basic_type(n));
          vlen_in_bytes = vn->as_Vector()->length_in_bytes();
        }
      } else {
        ShouldNotReachHere();
      }
      assert(vn != NULL, "sanity");
      _igvn.register_new_node_with_optimizer(vn);
      _phase->set_ctrl(vn, _phase->get_ctrl(p->at(0)));
      for (uint j = 0; j < p->size(); j++) {
        Node* pm = p->at(j);
        _igvn.replace_node(pm, vn);
      }
      _igvn._worklist.push(vn);

      if (vlen_in_bytes > max_vlen_in_bytes) {
        max_vlen_in_bytes = vlen_in_bytes;
      }
#ifdef ASSERT
      if (TraceNewVectors) {
        tty->print("new Vector node: ");
        vn->dump();
      }
#endif
    }
  }
  C->set_max_vector_size(max_vlen_in_bytes);
}

//------------------------------vector_opd---------------------------
// Create a vector operand for the nodes in pack p for operand: in(opd_idx)
Node* SuperWord::vector_opd(Node_List* p, int opd_idx) {
  Node* p0 = p->at(0);
  uint vlen = p->size();
  Node* opd = p0->in(opd_idx);

  if (same_inputs(p, opd_idx)) {
    if (opd->is_Vector() || opd->is_LoadVector()) {
      assert(((opd_idx != 2) || !VectorNode::is_shift(p0)), "shift's count can't be vector");
      return opd; // input is matching vector
    }
    if ((opd_idx == 2) && VectorNode::is_shift(p0)) {
      Compile* C = _phase->C;
      Node* cnt = opd;
      // Vector instructions do not mask shift count, do it here.
      juint mask = (p0->bottom_type() == TypeInt::INT) ? (BitsPerInt - 1) : (BitsPerLong - 1);
      const TypeInt* t = opd->find_int_type();
      if (t != NULL && t->is_con()) {
        juint shift = t->get_con();
        if (shift > mask) { // Unsigned cmp
          cnt = ConNode::make(TypeInt::make(shift & mask));
        }
      } else {
        if (t == NULL || t->_lo < 0 || t->_hi > (int)mask) {
          cnt = ConNode::make(TypeInt::make(mask));
          _igvn.register_new_node_with_optimizer(cnt);
          cnt = new AndINode(opd, cnt);
          _igvn.register_new_node_with_optimizer(cnt);
          _phase->set_ctrl(cnt, _phase->get_ctrl(opd));
        }
        assert(opd->bottom_type()->isa_int(), "int type only");
        // Move non constant shift count into vector register.
        cnt = VectorNode::shift_count(p0, cnt, vlen, velt_basic_type(p0));
      }
      if (cnt != opd) {
        _igvn.register_new_node_with_optimizer(cnt);
        _phase->set_ctrl(cnt, _phase->get_ctrl(opd));
      }
      return cnt;
    }
    assert(!opd->is_StoreVector(), "such vector is not expected here");
    // Convert scalar input to vector with the same number of elements as
    // p0's vector. Use p0's type because size of operand's container in
    // vector should match p0's size regardless operand's size.
    const Type* p0_t = velt_type(p0);
    VectorNode* vn = VectorNode::scalar2vector(opd, vlen, p0_t);

    _igvn.register_new_node_with_optimizer(vn);
    _phase->set_ctrl(vn, _phase->get_ctrl(opd));
#ifdef ASSERT
    if (TraceNewVectors) {
      tty->print("new Vector node: ");
      vn->dump();
    }
#endif
    return vn;
  }

  // Insert pack operation
  BasicType bt = velt_basic_type(p0);
  PackNode* pk = PackNode::make(opd, vlen, bt);
  DEBUG_ONLY( const BasicType opd_bt = opd->bottom_type()->basic_type(); )

  for (uint i = 1; i < vlen; i++) {
    Node* pi = p->at(i);
    Node* in = pi->in(opd_idx);
    assert(my_pack(in) == NULL, "Should already have been unpacked");
    assert(opd_bt == in->bottom_type()->basic_type(), "all same type");
    pk->add_opd(in);
  }
  _igvn.register_new_node_with_optimizer(pk);
  _phase->set_ctrl(pk, _phase->get_ctrl(opd));
#ifdef ASSERT
  if (TraceNewVectors) {
    tty->print("new Vector node: ");
    pk->dump();
  }
#endif
  return pk;
}

//------------------------------insert_extracts---------------------------
// If a use of pack p is not a vector use, then replace the
// use with an extract operation.
void SuperWord::insert_extracts(Node_List* p) {
  if (p->at(0)->is_Store()) return;
  assert(_n_idx_list.is_empty(), "empty (node,index) list");

  // Inspect each use of each pack member.  For each use that is
  // not a vector use, replace the use with an extract operation.

  for (uint i = 0; i < p->size(); i++) {
    Node* def = p->at(i);
    for (DUIterator_Fast jmax, j = def->fast_outs(jmax); j < jmax; j++) {
      Node* use = def->fast_out(j);
      for (uint k = 0; k < use->req(); k++) {
        Node* n = use->in(k);
        if (def == n) {
          if (!is_vector_use(use, k)) {
            _n_idx_list.push(use, k);
          }
        }
      }
    }
  }

  while (_n_idx_list.is_nonempty()) {
    Node* use = _n_idx_list.node();
    int   idx = _n_idx_list.index();
    _n_idx_list.pop();
    Node* def = use->in(idx);

    if (def->is_reduction()) continue;

    // Insert extract operation
    _igvn.hash_delete(def);
    int def_pos = alignment(def) / data_size(def);

    Node* ex = ExtractNode::make(def, def_pos, velt_basic_type(def));
    _igvn.register_new_node_with_optimizer(ex);
    _phase->set_ctrl(ex, _phase->get_ctrl(def));
    _igvn.replace_input_of(use, idx, ex);
    _igvn._worklist.push(def);

    bb_insert_after(ex, bb_idx(def));
    set_velt_type(ex, velt_type(def));
  }
}

//------------------------------is_vector_use---------------------------
// Is use->in(u_idx) a vector use?
bool SuperWord::is_vector_use(Node* use, int u_idx) {
  Node_List* u_pk = my_pack(use);
  if (u_pk == NULL) return false;
  if (use->is_reduction()) return true;
  Node* def = use->in(u_idx);
  Node_List* d_pk = my_pack(def);
  if (d_pk == NULL) {
    // check for scalar promotion
    Node* n = u_pk->at(0)->in(u_idx);
    for (uint i = 1; i < u_pk->size(); i++) {
      if (u_pk->at(i)->in(u_idx) != n) return false;
    }
    return true;
  }
  if (u_pk->size() != d_pk->size())
    return false;
  for (uint i = 0; i < u_pk->size(); i++) {
    Node* ui = u_pk->at(i);
    Node* di = d_pk->at(i);
    if (ui->in(u_idx) != di || alignment(ui) != alignment(di))
      return false;
  }
  return true;
}

//------------------------------construct_bb---------------------------
// Construct reverse postorder list of block members
bool SuperWord::construct_bb() {
  Node* entry = bb();

  assert(_stk.length() == 0,            "stk is empty");
  assert(_block.length() == 0,          "block is empty");
  assert(_data_entry.length() == 0,     "data_entry is empty");
  assert(_mem_slice_head.length() == 0, "mem_slice_head is empty");
  assert(_mem_slice_tail.length() == 0, "mem_slice_tail is empty");

  // Find non-control nodes with no inputs from within block,
  // create a temporary map from node _idx to bb_idx for use
  // by the visited and post_visited sets,
  // and count number of nodes in block.
  int bb_ct = 0;
  for (uint i = 0; i < lpt()->_body.size(); i++) {
    Node *n = lpt()->_body.at(i);
    set_bb_idx(n, i); // Create a temporary map
    if (in_bb(n)) {
      if (n->is_LoadStore() || n->is_MergeMem() ||
          (n->is_Proj() && !n->as_Proj()->is_CFG())) {
        // Bailout if the loop has LoadStore, MergeMem or data Proj
        // nodes. Superword optimization does not work with them.
        return false;
      }
      bb_ct++;
      if (!n->is_CFG()) {
        bool found = false;
        for (uint j = 0; j < n->req(); j++) {
          Node* def = n->in(j);
          if (def && in_bb(def)) {
            found = true;
            break;
          }
        }
        if (!found) {
          assert(n != entry, "can't be entry");
          _data_entry.push(n);
        }
      }
    }
  }

  // Find memory slices (head and tail)
  for (DUIterator_Fast imax, i = lp()->fast_outs(imax); i < imax; i++) {
    Node *n = lp()->fast_out(i);
    if (in_bb(n) && (n->is_Phi() && n->bottom_type() == Type::MEMORY)) {
      Node* n_tail  = n->in(LoopNode::LoopBackControl);
      if (n_tail != n->in(LoopNode::EntryControl)) {
        if (!n_tail->is_Mem()) {
          assert(n_tail->is_Mem(), err_msg_res("unexpected node for memory slice: %s", n_tail->Name()));
          return false; // Bailout
        }
        _mem_slice_head.push(n);
        _mem_slice_tail.push(n_tail);
      }
    }
  }

  // Create an RPO list of nodes in block

  visited_clear();
  post_visited_clear();

  // Push all non-control nodes with no inputs from within block, then control entry
  for (int j = 0; j < _data_entry.length(); j++) {
    Node* n = _data_entry.at(j);
    visited_set(n);
    _stk.push(n);
  }
  visited_set(entry);
  _stk.push(entry);

  // Do a depth first walk over out edges
  int rpo_idx = bb_ct - 1;
  int size;
  int reduction_uses = 0;
  while ((size = _stk.length()) > 0) {
    Node* n = _stk.top(); // Leave node on stack
    if (!visited_test_set(n)) {
      // forward arc in graph
    } else if (!post_visited_test(n)) {
      // cross or back arc
      for (DUIterator_Fast imax, i = n->fast_outs(imax); i < imax; i++) {
        Node *use = n->fast_out(i);
        if (in_bb(use) && !visited_test(use) &&
            // Don't go around backedge
            (!use->is_Phi() || n == entry)) {
          if (use->is_reduction()) {
            // First see if we can map the reduction on the given system we are on, then
            // make a data entry operation for each reduction we see.
            BasicType bt = use->bottom_type()->basic_type();
            if (ReductionNode::implemented(use->Opcode(), Matcher::min_vector_size(bt), bt)) {
              reduction_uses++;
            }
          }
          _stk.push(use);
        }
      }
      if (_stk.length() == size) {
        // There were no additional uses, post visit node now
        _stk.pop(); // Remove node from stack
        assert(rpo_idx >= 0, "");
        _block.at_put_grow(rpo_idx, n);
        rpo_idx--;
        post_visited_set(n);
        assert(rpo_idx >= 0 || _stk.is_empty(), "");
      }
    } else {
      _stk.pop(); // Remove post-visited node from stack
    }
  }

  // Create real map of block indices for nodes
  for (int j = 0; j < _block.length(); j++) {
    Node* n = _block.at(j);
    set_bb_idx(n, j);
  }

  // Ensure extra info is allocated.
  initialize_bb();

#ifndef PRODUCT
  if (TraceSuperWord) {
    print_bb();
    tty->print_cr("\ndata entry nodes: %s", _data_entry.length() > 0 ? "" : "NONE");
    for (int m = 0; m < _data_entry.length(); m++) {
      tty->print("%3d ", m);
      _data_entry.at(m)->dump();
    }
    tty->print_cr("\nmemory slices: %s", _mem_slice_head.length() > 0 ? "" : "NONE");
    for (int m = 0; m < _mem_slice_head.length(); m++) {
      tty->print("%3d ", m); _mem_slice_head.at(m)->dump();
      tty->print("    ");    _mem_slice_tail.at(m)->dump();
    }
  }
#endif
  assert(rpo_idx == -1 && bb_ct == _block.length(), "all block members found");
  return (_mem_slice_head.length() > 0) || (reduction_uses > 0) || (_data_entry.length() > 0);
}

//------------------------------initialize_bb---------------------------
// Initialize per node info
void SuperWord::initialize_bb() {
  Node* last = _block.at(_block.length() - 1);
  grow_node_info(bb_idx(last));
}

//------------------------------bb_insert_after---------------------------
// Insert n into block after pos
void SuperWord::bb_insert_after(Node* n, int pos) {
  int n_pos = pos + 1;
  // Make room
  for (int i = _block.length() - 1; i >= n_pos; i--) {
    _block.at_put_grow(i+1, _block.at(i));
  }
  for (int j = _node_info.length() - 1; j >= n_pos; j--) {
    _node_info.at_put_grow(j+1, _node_info.at(j));
  }
  // Set value
  _block.at_put_grow(n_pos, n);
  _node_info.at_put_grow(n_pos, SWNodeInfo::initial);
  // Adjust map from node->_idx to _block index
  for (int i = n_pos; i < _block.length(); i++) {
    set_bb_idx(_block.at(i), i);
  }
}

//------------------------------compute_max_depth---------------------------
// Compute max depth for expressions from beginning of block
// Use to prune search paths during test for independence.
void SuperWord::compute_max_depth() {
  int ct = 0;
  bool again;
  do {
    again = false;
    for (int i = 0; i < _block.length(); i++) {
      Node* n = _block.at(i);
      if (!n->is_Phi()) {
        int d_orig = depth(n);
        int d_in   = 0;
        for (DepPreds preds(n, _dg); !preds.done(); preds.next()) {
          Node* pred = preds.current();
          if (in_bb(pred)) {
            d_in = MAX2(d_in, depth(pred));
          }
        }
        if (d_in + 1 != d_orig) {
          set_depth(n, d_in + 1);
          again = true;
        }
      }
    }
    ct++;
  } while (again);
#ifndef PRODUCT
  if (TraceSuperWord && Verbose)
    tty->print_cr("compute_max_depth iterated: %d times", ct);
#endif
}

//-------------------------compute_vector_element_type-----------------------
// Compute necessary vector element type for expressions
// This propagates backwards a narrower integer type when the
// upper bits of the value are not needed.
// Example:  char a,b,c;  a = b + c;
// Normally the type of the add is integer, but for packed character
// operations the type of the add needs to be char.
void SuperWord::compute_vector_element_type() {
#ifndef PRODUCT
  if (TraceSuperWord && Verbose)
    tty->print_cr("\ncompute_velt_type:");
#endif

  // Initial type
  for (int i = 0; i < _block.length(); i++) {
    Node* n = _block.at(i);
    set_velt_type(n, container_type(n));
  }

  // Propagate integer narrowed type backwards through operations
  // that don't depend on higher order bits
  for (int i = _block.length() - 1; i >= 0; i--) {
    Node* n = _block.at(i);
    // Only integer types need be examined
    const Type* vtn = velt_type(n);
    if (vtn->basic_type() == T_INT) {
      uint start, end;
      VectorNode::vector_operands(n, &start, &end);

      for (uint j = start; j < end; j++) {
        Node* in  = n->in(j);
        // Don't propagate through a memory
        if (!in->is_Mem() && in_bb(in) && velt_type(in)->basic_type() == T_INT &&
            data_size(n) < data_size(in)) {
          bool same_type = true;
          for (DUIterator_Fast kmax, k = in->fast_outs(kmax); k < kmax; k++) {
            Node *use = in->fast_out(k);
            if (!in_bb(use) || !same_velt_type(use, n)) {
              same_type = false;
              break;
            }
          }
          if (same_type) {
            // For right shifts of small integer types (bool, byte, char, short)
            // we need precise information about sign-ness. Only Load nodes have
            // this information because Store nodes are the same for signed and
            // unsigned values. And any arithmetic operation after a load may
            // expand a value to signed Int so such right shifts can't be used
            // because vector elements do not have upper bits of Int.
            const Type* vt = vtn;
            if (VectorNode::is_shift(in)) {
              Node* load = in->in(1);
              if (load->is_Load() && in_bb(load) && (velt_type(load)->basic_type() == T_INT)) {
                vt = velt_type(load);
              } else if (in->Opcode() != Op_LShiftI) {
                // Widen type to Int to avoid creation of right shift vector
                // (align + data_size(s1) check in stmts_can_pack() will fail).
                // Note, left shifts work regardless type.
                vt = TypeInt::INT;
              }
            }
            set_velt_type(in, vt);
          }
        }
      }
    }
  }
#ifndef PRODUCT
  if (TraceSuperWord && Verbose) {
    for (int i = 0; i < _block.length(); i++) {
      Node* n = _block.at(i);
      velt_type(n)->dump();
      tty->print("\t");
      n->dump();
    }
  }
#endif
}

//------------------------------memory_alignment---------------------------
// Alignment within a vector memory reference
int SuperWord::memory_alignment(MemNode* s, int iv_adjust) {
  SWPointer p(s, this);
  if (!p.valid()) {
    return bottom_align;
  }
  int vw = vector_width_in_bytes(s);
  if (vw < 2) {
    return bottom_align; // No vectors for this type
  }
  int offset  = p.offset_in_bytes();
  offset     += iv_adjust*p.memory_size();
  int off_rem = offset % vw;
  int off_mod = off_rem >= 0 ? off_rem : off_rem + vw;
  return off_mod;
}

//---------------------------container_type---------------------------
// Smallest type containing range of values
const Type* SuperWord::container_type(Node* n) {
  if (n->is_Mem()) {
    BasicType bt = n->as_Mem()->memory_type();
    if (n->is_Store() && (bt == T_CHAR)) {
      // Use T_SHORT type instead of T_CHAR for stored values because any
      // preceding arithmetic operation extends values to signed Int.
      bt = T_SHORT;
    }
    if (n->Opcode() == Op_LoadUB) {
      // Adjust type for unsigned byte loads, it is important for right shifts.
      // T_BOOLEAN is used because there is no basic type representing type
      // TypeInt::UBYTE. Use of T_BOOLEAN for vectors is fine because only
      // size (one byte) and sign is important.
      bt = T_BOOLEAN;
    }
    return Type::get_const_basic_type(bt);
  }
  const Type* t = _igvn.type(n);
  if (t->basic_type() == T_INT) {
    // A narrow type of arithmetic operations will be determined by
    // propagating the type of memory operations.
    return TypeInt::INT;
  }
  return t;
}

bool SuperWord::same_velt_type(Node* n1, Node* n2) {
  const Type* vt1 = velt_type(n1);
  const Type* vt2 = velt_type(n2);
  if (vt1->basic_type() == T_INT && vt2->basic_type() == T_INT) {
    // Compare vectors element sizes for integer types.
    return data_size(n1) == data_size(n2);
  }
  return vt1 == vt2;
}

//------------------------------in_packset---------------------------
// Are s1 and s2 in a pack pair and ordered as s1,s2?
bool SuperWord::in_packset(Node* s1, Node* s2) {
  for (int i = 0; i < _packset.length(); i++) {
    Node_List* p = _packset.at(i);
    assert(p->size() == 2, "must be");
    if (p->at(0) == s1 && p->at(p->size()-1) == s2) {
      return true;
    }
  }
  return false;
}

//------------------------------in_pack---------------------------
// Is s in pack p?
Node_List* SuperWord::in_pack(Node* s, Node_List* p) {
  for (uint i = 0; i < p->size(); i++) {
    if (p->at(i) == s) {
      return p;
    }
  }
  return NULL;
}

//------------------------------remove_pack_at---------------------------
// Remove the pack at position pos in the packset
void SuperWord::remove_pack_at(int pos) {
  Node_List* p = _packset.at(pos);
  for (uint i = 0; i < p->size(); i++) {
    Node* s = p->at(i);
    set_my_pack(s, NULL);
  }
  _packset.remove_at(pos);
}

void SuperWord::packset_sort(int n) {
  // simple bubble sort so that we capitalize with O(n) when its already sorted
  while (n != 0) {
    bool swapped = false;
    for (int i = 1; i < n; i++) {
      Node_List* q_low = _packset.at(i-1);
      Node_List* q_i = _packset.at(i);

      // only swap when we find something to swap
      if (alignment(q_low->at(0)) > alignment(q_i->at(0))) {
        Node_List* t = q_i;
        *(_packset.adr_at(i)) = q_low;
        *(_packset.adr_at(i-1)) = q_i;
        swapped = true;
      }
    }
    if (swapped == false) break;
    n--;
  }
}

//------------------------------executed_first---------------------------
// Return the node executed first in pack p.  Uses the RPO block list
// to determine order.
Node* SuperWord::executed_first(Node_List* p) {
  Node* n = p->at(0);
  int n_rpo = bb_idx(n);
  for (uint i = 1; i < p->size(); i++) {
    Node* s = p->at(i);
    int s_rpo = bb_idx(s);
    if (s_rpo < n_rpo) {
      n = s;
      n_rpo = s_rpo;
    }
  }
  return n;
}

//------------------------------executed_last---------------------------
// Return the node executed last in pack p.
Node* SuperWord::executed_last(Node_List* p) {
  Node* n = p->at(0);
  int n_rpo = bb_idx(n);
  for (uint i = 1; i < p->size(); i++) {
    Node* s = p->at(i);
    int s_rpo = bb_idx(s);
    if (s_rpo > n_rpo) {
      n = s;
      n_rpo = s_rpo;
    }
  }
  return n;
}

//----------------------------align_initial_loop_index---------------------------
// Adjust pre-loop limit so that in main loop, a load/store reference
// to align_to_ref will be a position zero in the vector.
//   (iv + k) mod vector_align == 0
void SuperWord::align_initial_loop_index(MemNode* align_to_ref) {
  CountedLoopNode *main_head = lp()->as_CountedLoop();
  assert(main_head->is_main_loop(), "");
  CountedLoopEndNode* pre_end = get_pre_loop_end(main_head);
  assert(pre_end != NULL, "we must have a correct pre-loop");
  Node *pre_opaq1 = pre_end->limit();
  assert(pre_opaq1->Opcode() == Op_Opaque1, "");
  Opaque1Node *pre_opaq = (Opaque1Node*)pre_opaq1;
  Node *lim0 = pre_opaq->in(1);

  // Where we put new limit calculations
  Node *pre_ctrl = pre_end->loopnode()->in(LoopNode::EntryControl);

  // Ensure the original loop limit is available from the
  // pre-loop Opaque1 node.
  Node *orig_limit = pre_opaq->original_loop_limit();
  assert(orig_limit != NULL && _igvn.type(orig_limit) != Type::TOP, "");

  SWPointer align_to_ref_p(align_to_ref, this);
  assert(align_to_ref_p.valid(), "sanity");

  // Given:
  //     lim0 == original pre loop limit
  //     V == v_align (power of 2)
  //     invar == extra invariant piece of the address expression
  //     e == offset [ +/- invar ]
  //
  // When reassociating expressions involving '%' the basic rules are:
  //     (a - b) % k == 0   =>  a % k == b % k
  // and:
  //     (a + b) % k == 0   =>  a % k == (k - b) % k
  //
  // For stride > 0 && scale > 0,
  //   Derive the new pre-loop limit "lim" such that the two constraints:
  //     (1) lim = lim0 + N           (where N is some positive integer < V)
  //     (2) (e + lim) % V == 0
  //   are true.
  //
  //   Substituting (1) into (2),
  //     (e + lim0 + N) % V == 0
  //   solve for N:
  //     N = (V - (e + lim0)) % V
  //   substitute back into (1), so that new limit
  //     lim = lim0 + (V - (e + lim0)) % V
  //
  // For stride > 0 && scale < 0
  //   Constraints:
  //     lim = lim0 + N
  //     (e - lim) % V == 0
  //   Solving for lim:
  //     (e - lim0 - N) % V == 0
  //     N = (e - lim0) % V
  //     lim = lim0 + (e - lim0) % V
  //
  // For stride < 0 && scale > 0
  //   Constraints:
  //     lim = lim0 - N
  //     (e + lim) % V == 0
  //   Solving for lim:
  //     (e + lim0 - N) % V == 0
  //     N = (e + lim0) % V
  //     lim = lim0 - (e + lim0) % V
  //
  // For stride < 0 && scale < 0
  //   Constraints:
  //     lim = lim0 - N
  //     (e - lim) % V == 0
  //   Solving for lim:
  //     (e - lim0 + N) % V == 0
  //     N = (V - (e - lim0)) % V
  //     lim = lim0 - (V - (e - lim0)) % V

  int vw = vector_width_in_bytes(align_to_ref);
  int stride   = iv_stride();
  int scale    = align_to_ref_p.scale_in_bytes();
  int elt_size = align_to_ref_p.memory_size();
  int v_align  = vw / elt_size;
  assert(v_align > 1, "sanity");
  int offset   = align_to_ref_p.offset_in_bytes() / elt_size;
  Node *offsn  = _igvn.intcon(offset);

  Node *e = offsn;
  if (align_to_ref_p.invar() != NULL) {
    // incorporate any extra invariant piece producing (offset +/- invar) >>> log2(elt)
    Node* log2_elt = _igvn.intcon(exact_log2(elt_size));
    Node* aref     = new URShiftINode(align_to_ref_p.invar(), log2_elt);
    _igvn.register_new_node_with_optimizer(aref);
    _phase->set_ctrl(aref, pre_ctrl);
    if (align_to_ref_p.negate_invar()) {
      e = new SubINode(e, aref);
    } else {
      e = new AddINode(e, aref);
    }
    _igvn.register_new_node_with_optimizer(e);
    _phase->set_ctrl(e, pre_ctrl);
  }
  if (vw > ObjectAlignmentInBytes) {
    // incorporate base e +/- base && Mask >>> log2(elt)
    Node* xbase = new CastP2XNode(NULL, align_to_ref_p.base());
    _igvn.register_new_node_with_optimizer(xbase);
#ifdef _LP64
    xbase  = new ConvL2INode(xbase);
    _igvn.register_new_node_with_optimizer(xbase);
#endif
    Node* mask = _igvn.intcon(vw-1);
    Node* masked_xbase  = new AndINode(xbase, mask);
    _igvn.register_new_node_with_optimizer(masked_xbase);
    Node* log2_elt = _igvn.intcon(exact_log2(elt_size));
    Node* bref     = new URShiftINode(masked_xbase, log2_elt);
    _igvn.register_new_node_with_optimizer(bref);
    _phase->set_ctrl(bref, pre_ctrl);
    e = new AddINode(e, bref);
    _igvn.register_new_node_with_optimizer(e);
    _phase->set_ctrl(e, pre_ctrl);
  }

  // compute e +/- lim0
  if (scale < 0) {
    e = new SubINode(e, lim0);
  } else {
    e = new AddINode(e, lim0);
  }
  _igvn.register_new_node_with_optimizer(e);
  _phase->set_ctrl(e, pre_ctrl);

  if (stride * scale > 0) {
    // compute V - (e +/- lim0)
    Node* va  = _igvn.intcon(v_align);
    e = new SubINode(va, e);
    _igvn.register_new_node_with_optimizer(e);
    _phase->set_ctrl(e, pre_ctrl);
  }
  // compute N = (exp) % V
  Node* va_msk = _igvn.intcon(v_align - 1);
  Node* N = new AndINode(e, va_msk);
  _igvn.register_new_node_with_optimizer(N);
  _phase->set_ctrl(N, pre_ctrl);

  //   substitute back into (1), so that new limit
  //     lim = lim0 + N
  Node* lim;
  if (stride < 0) {
    lim = new SubINode(lim0, N);
  } else {
    lim = new AddINode(lim0, N);
  }
  _igvn.register_new_node_with_optimizer(lim);
  _phase->set_ctrl(lim, pre_ctrl);
  Node* constrained =
    (stride > 0) ? (Node*) new MinINode(lim, orig_limit)
                 : (Node*) new MaxINode(lim, orig_limit);
  _igvn.register_new_node_with_optimizer(constrained);
  _phase->set_ctrl(constrained, pre_ctrl);
  _igvn.hash_delete(pre_opaq);
  pre_opaq->set_req(1, constrained);
}

//----------------------------get_pre_loop_end---------------------------
// Find pre loop end from main loop.  Returns null if none.
CountedLoopEndNode* SuperWord::get_pre_loop_end(CountedLoopNode *cl) {
  Node *ctrl = cl->in(LoopNode::EntryControl);
  if (!ctrl->is_IfTrue() && !ctrl->is_IfFalse()) return NULL;
  Node *iffm = ctrl->in(0);
  if (!iffm->is_If()) return NULL;
  Node *p_f = iffm->in(0);
  if (!p_f->is_IfFalse()) return NULL;
  if (!p_f->in(0)->is_CountedLoopEnd()) return NULL;
  CountedLoopEndNode *pre_end = p_f->in(0)->as_CountedLoopEnd();
  CountedLoopNode* loop_node = pre_end->loopnode();
  if (loop_node == NULL || !loop_node->is_pre_loop()) return NULL;
  return pre_end;
}


//------------------------------init---------------------------
void SuperWord::init() {
  _dg.init();
  _packset.clear();
  _disjoint_ptrs.clear();
  _block.clear();
  _data_entry.clear();
  _mem_slice_head.clear();
  _mem_slice_tail.clear();
  _iteration_first.clear();
  _iteration_last.clear();
  _node_info.clear();
  _align_to_ref = NULL;
  _lpt = NULL;
  _lp = NULL;
  _bb = NULL;
  _iv = NULL;
  _race_possible = 0;
  _num_work_vecs = 0;
  _num_reductions = 0;
}

//------------------------------restart---------------------------
void SuperWord::restart() {
  _dg.init();
  _packset.clear();
  _disjoint_ptrs.clear();
  _block.clear();
  _data_entry.clear();
  _mem_slice_head.clear();
  _mem_slice_tail.clear();
  _node_info.clear();
}

//------------------------------print_packset---------------------------
void SuperWord::print_packset() {
#ifndef PRODUCT
  tty->print_cr("packset");
  for (int i = 0; i < _packset.length(); i++) {
    tty->print_cr("Pack: %d", i);
    Node_List* p = _packset.at(i);
    print_pack(p);
  }
#endif
}

//------------------------------print_pack---------------------------
void SuperWord::print_pack(Node_List* p) {
  for (uint i = 0; i < p->size(); i++) {
    print_stmt(p->at(i));
  }
}

//------------------------------print_bb---------------------------
void SuperWord::print_bb() {
#ifndef PRODUCT
  tty->print_cr("\nBlock");
  for (int i = 0; i < _block.length(); i++) {
    Node* n = _block.at(i);
    tty->print("%d ", i);
    if (n) {
      n->dump();
    }
  }
#endif
}

//------------------------------print_stmt---------------------------
void SuperWord::print_stmt(Node* s) {
#ifndef PRODUCT
  tty->print(" align: %d \t", alignment(s));
  s->dump();
#endif
}

//------------------------------blank---------------------------
char* SuperWord::blank(uint depth) {
  static char blanks[101];
  assert(depth < 101, "too deep");
  for (uint i = 0; i < depth; i++) blanks[i] = ' ';
  blanks[depth] = '\0';
  return blanks;
}


//==============================SWPointer===========================

//----------------------------SWPointer------------------------
SWPointer::SWPointer(MemNode* mem, SuperWord* slp) :
  _mem(mem), _slp(slp),  _base(NULL),  _adr(NULL),
  _scale(0), _offset(0), _invar(NULL), _negate_invar(false) {

  Node* adr = mem->in(MemNode::Address);
  if (!adr->is_AddP()) {
    assert(!valid(), "too complex");
    return;
  }
  // Match AddP(base, AddP(ptr, k*iv [+ invariant]), constant)
  Node* base = adr->in(AddPNode::Base);
  //unsafe reference could not be aligned appropriately without runtime checking
  if (base == NULL || base->bottom_type() == Type::TOP) {
    assert(!valid(), "unsafe access");
    return;
  }
  for (int i = 0; i < 3; i++) {
    if (!scaled_iv_plus_offset(adr->in(AddPNode::Offset))) {
      assert(!valid(), "too complex");
      return;
    }
    adr = adr->in(AddPNode::Address);
    if (base == adr || !adr->is_AddP()) {
      break; // stop looking at addp's
    }
  }
  _base = base;
  _adr  = adr;
  assert(valid(), "Usable");
}

// Following is used to create a temporary object during
// the pattern match of an address expression.
SWPointer::SWPointer(SWPointer* p) :
  _mem(p->_mem), _slp(p->_slp),  _base(NULL),  _adr(NULL),
  _scale(0), _offset(0), _invar(NULL), _negate_invar(false) {}

//------------------------scaled_iv_plus_offset--------------------
// Match: k*iv + offset
// where: k is a constant that maybe zero, and
//        offset is (k2 [+/- invariant]) where k2 maybe zero and invariant is optional
bool SWPointer::scaled_iv_plus_offset(Node* n) {
  if (scaled_iv(n)) {
    return true;
  }
  if (offset_plus_k(n)) {
    return true;
  }
  int opc = n->Opcode();
  if (opc == Op_AddI) {
    if (scaled_iv(n->in(1)) && offset_plus_k(n->in(2))) {
      return true;
    }
    if (scaled_iv(n->in(2)) && offset_plus_k(n->in(1))) {
      return true;
    }
  } else if (opc == Op_SubI) {
    if (scaled_iv(n->in(1)) && offset_plus_k(n->in(2), true)) {
      return true;
    }
    if (scaled_iv(n->in(2)) && offset_plus_k(n->in(1))) {
      _scale *= -1;
      return true;
    }
  }
  return false;
}

//----------------------------scaled_iv------------------------
// Match: k*iv where k is a constant that's not zero
bool SWPointer::scaled_iv(Node* n) {
  if (_scale != 0) {
    return false;  // already found a scale
  }
  if (n == iv()) {
    _scale = 1;
    return true;
  }
  int opc = n->Opcode();
  if (opc == Op_MulI) {
    if (n->in(1) == iv() && n->in(2)->is_Con()) {
      _scale = n->in(2)->get_int();
      return true;
    } else if (n->in(2) == iv() && n->in(1)->is_Con()) {
      _scale = n->in(1)->get_int();
      return true;
    }
  } else if (opc == Op_LShiftI) {
    if (n->in(1) == iv() && n->in(2)->is_Con()) {
      _scale = 1 << n->in(2)->get_int();
      return true;
    }
  } else if (opc == Op_ConvI2L) {
    if (scaled_iv_plus_offset(n->in(1))) {
      return true;
    }
  } else if (opc == Op_LShiftL) {
    if (!has_iv() && _invar == NULL) {
      // Need to preserve the current _offset value, so
      // create a temporary object for this expression subtree.
      // Hacky, so should re-engineer the address pattern match.
      SWPointer tmp(this);
      if (tmp.scaled_iv_plus_offset(n->in(1))) {
        if (tmp._invar == NULL) {
          int mult = 1 << n->in(2)->get_int();
          _scale   = tmp._scale  * mult;
          _offset += tmp._offset * mult;
          return true;
        }
      }
    }
  }
  return false;
}

//----------------------------offset_plus_k------------------------
// Match: offset is (k [+/- invariant])
// where k maybe zero and invariant is optional, but not both.
bool SWPointer::offset_plus_k(Node* n, bool negate) {
  int opc = n->Opcode();
  if (opc == Op_ConI) {
    _offset += negate ? -(n->get_int()) : n->get_int();
    return true;
  } else if (opc == Op_ConL) {
    // Okay if value fits into an int
    const TypeLong* t = n->find_long_type();
    if (t->higher_equal(TypeLong::INT)) {
      jlong loff = n->get_long();
      jint  off  = (jint)loff;
      _offset += negate ? -off : loff;
      return true;
    }
    return false;
  }
  if (_invar != NULL) return false; // already have an invariant
  if (opc == Op_AddI) {
    if (n->in(2)->is_Con() && invariant(n->in(1))) {
      _negate_invar = negate;
      _invar = n->in(1);
      _offset += negate ? -(n->in(2)->get_int()) : n->in(2)->get_int();
      return true;
    } else if (n->in(1)->is_Con() && invariant(n->in(2))) {
      _offset += negate ? -(n->in(1)->get_int()) : n->in(1)->get_int();
      _negate_invar = negate;
      _invar = n->in(2);
      return true;
    }
  }
  if (opc == Op_SubI) {
    if (n->in(2)->is_Con() && invariant(n->in(1))) {
      _negate_invar = negate;
      _invar = n->in(1);
      _offset += !negate ? -(n->in(2)->get_int()) : n->in(2)->get_int();
      return true;
    } else if (n->in(1)->is_Con() && invariant(n->in(2))) {
      _offset += negate ? -(n->in(1)->get_int()) : n->in(1)->get_int();
      _negate_invar = !negate;
      _invar = n->in(2);
      return true;
    }
  }
  if (invariant(n)) {
    _negate_invar = negate;
    _invar = n;
    return true;
  }
  return false;
}

//----------------------------print------------------------
void SWPointer::print() {
#ifndef PRODUCT
  tty->print("base: %d  adr: %d  scale: %d  offset: %d  invar: %c%d\n",
             _base != NULL ? _base->_idx : 0,
             _adr  != NULL ? _adr->_idx  : 0,
             _scale, _offset,
             _negate_invar?'-':'+',
             _invar != NULL ? _invar->_idx : 0);
#endif
}

// ========================= OrderedPair =====================

const OrderedPair OrderedPair::initial;

// ========================= SWNodeInfo =====================

const SWNodeInfo SWNodeInfo::initial;


// ============================ DepGraph ===========================

//------------------------------make_node---------------------------
// Make a new dependence graph node for an ideal node.
DepMem* DepGraph::make_node(Node* node) {
  DepMem* m = new (_arena) DepMem(node);
  if (node != NULL) {
    assert(_map.at_grow(node->_idx) == NULL, "one init only");
    _map.at_put_grow(node->_idx, m);
  }
  return m;
}

//------------------------------make_edge---------------------------
// Make a new dependence graph edge from dpred -> dsucc
DepEdge* DepGraph::make_edge(DepMem* dpred, DepMem* dsucc) {
  DepEdge* e = new (_arena) DepEdge(dpred, dsucc, dsucc->in_head(), dpred->out_head());
  dpred->set_out_head(e);
  dsucc->set_in_head(e);
  return e;
}

// ========================== DepMem ========================

//------------------------------in_cnt---------------------------
int DepMem::in_cnt() {
  int ct = 0;
  for (DepEdge* e = _in_head; e != NULL; e = e->next_in()) ct++;
  return ct;
}

//------------------------------out_cnt---------------------------
int DepMem::out_cnt() {
  int ct = 0;
  for (DepEdge* e = _out_head; e != NULL; e = e->next_out()) ct++;
  return ct;
}

//------------------------------print-----------------------------
void DepMem::print() {
#ifndef PRODUCT
  tty->print("  DepNode %d (", _node->_idx);
  for (DepEdge* p = _in_head; p != NULL; p = p->next_in()) {
    Node* pred = p->pred()->node();
    tty->print(" %d", pred != NULL ? pred->_idx : 0);
  }
  tty->print(") [");
  for (DepEdge* s = _out_head; s != NULL; s = s->next_out()) {
    Node* succ = s->succ()->node();
    tty->print(" %d", succ != NULL ? succ->_idx : 0);
  }
  tty->print_cr(" ]");
#endif
}

// =========================== DepEdge =========================

//------------------------------DepPreds---------------------------
void DepEdge::print() {
#ifndef PRODUCT
  tty->print_cr("DepEdge: %d [ %d ]", _pred->node()->_idx, _succ->node()->_idx);
#endif
}

// =========================== DepPreds =========================
// Iterator over predecessor edges in the dependence graph.

//------------------------------DepPreds---------------------------
DepPreds::DepPreds(Node* n, DepGraph& dg) {
  _n = n;
  _done = false;
  if (_n->is_Store() || _n->is_Load()) {
    _next_idx = MemNode::Address;
    _end_idx  = n->req();
    _dep_next = dg.dep(_n)->in_head();
  } else if (_n->is_Mem()) {
    _next_idx = 0;
    _end_idx  = 0;
    _dep_next = dg.dep(_n)->in_head();
  } else {
    _next_idx = 1;
    _end_idx  = _n->req();
    _dep_next = NULL;
  }
  next();
}

//------------------------------next---------------------------
void DepPreds::next() {
  if (_dep_next != NULL) {
    _current  = _dep_next->pred()->node();
    _dep_next = _dep_next->next_in();
  } else if (_next_idx < _end_idx) {
    _current  = _n->in(_next_idx++);
  } else {
    _done = true;
  }
}

// =========================== DepSuccs =========================
// Iterator over successor edges in the dependence graph.

//------------------------------DepSuccs---------------------------
DepSuccs::DepSuccs(Node* n, DepGraph& dg) {
  _n = n;
  _done = false;
  if (_n->is_Load()) {
    _next_idx = 0;
    _end_idx  = _n->outcnt();
    _dep_next = dg.dep(_n)->out_head();
  } else if (_n->is_Mem() || _n->is_Phi() && _n->bottom_type() == Type::MEMORY) {
    _next_idx = 0;
    _end_idx  = 0;
    _dep_next = dg.dep(_n)->out_head();
  } else {
    _next_idx = 0;
    _end_idx  = _n->outcnt();
    _dep_next = NULL;
  }
  next();
}

//-------------------------------next---------------------------
void DepSuccs::next() {
  if (_dep_next != NULL) {
    _current  = _dep_next->succ()->node();
    _dep_next = _dep_next->next_out();
  } else if (_next_idx < _end_idx) {
    _current  = _n->raw_out(_next_idx++);
  } else {
    _done = true;
  }
}

//
// --------------------------------- vectorization/simd -----------------------------------
//
Node*  SuperWord::find_phi_for_mem_dep(LoadNode* ld) {
  assert(in_bb(ld), "must be in block");
  if (_clone_map.gen(ld->_idx) == _ii_first) {
#ifndef PRODUCT
    if (_vector_loop_debug) {
      tty->print_cr("SuperWord::find_phi_for_mem_dep _clone_map.gen(ld->_idx)=%d",
                    _clone_map.gen(ld->_idx));
    }
#endif
    return NULL; //we think that any ld in the first gen being vectorizable
  }

  Node* mem = ld->in(MemNode::Memory);
  if (mem->outcnt() <= 1) {
    // we don't want to remove the only edge from mem node to load
#ifndef PRODUCT
    if (_vector_loop_debug) {
      tty->print_cr("SuperWord::find_phi_for_mem_dep input node %d to load %d has no other outputs and edge mem->load cannot be removed",
                    mem->_idx, ld->_idx);
      ld->dump();
      mem->dump();
    }
#endif
    return NULL;
  }
  if (!in_bb(mem) || _clone_map.gen(mem->_idx) == _clone_map.gen(ld->_idx)) {
#ifndef PRODUCT
    if (_vector_loop_debug) {
      tty->print_cr("SuperWord::find_phi_for_mem_dep _clone_map.gen(mem->_idx)=%d",
                    _clone_map.gen(mem->_idx));
    }
#endif
    return NULL; // does not depend on loop volatile node or depends on the same generation
  }

  //otherwise first node should depend on mem-phi
  Node* first = first_node(ld);
  assert(first->is_Load(), "must be Load");
  Node* phi = first->as_Load()->in(MemNode::Memory);
  if (!phi->is_Phi() || phi->bottom_type() != Type::MEMORY) {
#ifndef PRODUCT
    if (_vector_loop_debug) {
      tty->print_cr("SuperWord::find_phi_for_mem_dep load is not vectorizable node, since it's `first` does not take input from mem phi");
      ld->dump();
      first->dump();
    }
#endif
    return NULL;
  }

  Node* tail = 0;
  for (int m = 0; m < _mem_slice_head.length(); m++) {
    if (_mem_slice_head.at(m) == phi) {
      tail = _mem_slice_tail.at(m);
    }
  }
  if (tail == 0) { //test that found phi is in the list  _mem_slice_head
#ifndef PRODUCT
    if (_vector_loop_debug) {
      tty->print_cr("SuperWord::find_phi_for_mem_dep load %d is not vectorizable node, its phi %d is not _mem_slice_head",
                    ld->_idx, phi->_idx);
      ld->dump();
      phi->dump();
    }
#endif
    return NULL;
  }

  // now all conditions are met
  return phi;
}

Node* SuperWord::first_node(Node* nd) {
  for (int ii = 0; ii < _iteration_first.length(); ii++) {
    Node* nnn = _iteration_first.at(ii);
    if (_clone_map.idx(nnn->_idx) == _clone_map.idx(nd->_idx)) {
#ifndef PRODUCT
      if (_vector_loop_debug) {
        tty->print_cr("SuperWord::first_node: %d is the first iteration node for %d (_clone_map.idx(nnn->_idx) = %d)",
                      nnn->_idx, nd->_idx, _clone_map.idx(nnn->_idx));
      }
#endif
      return nnn;
    }
  }

#ifndef PRODUCT
  if (_vector_loop_debug) {
    tty->print_cr("SuperWord::first_node: did not find first iteration node for %d (_clone_map.idx(nd->_idx)=%d)",
                  nd->_idx, _clone_map.idx(nd->_idx));
  }
#endif
  return 0;
}

Node* SuperWord::last_node(Node* nd) {
  for (int ii = 0; ii < _iteration_last.length(); ii++) {
    Node* nnn = _iteration_last.at(ii);
    if (_clone_map.idx(nnn->_idx) == _clone_map.idx(nd->_idx)) {
#ifndef PRODUCT
      if (_vector_loop_debug) {
        tty->print_cr("SuperWord::last_node _clone_map.idx(nnn->_idx)=%d, _clone_map.idx(nd->_idx)=%d",
                      _clone_map.idx(nnn->_idx), _clone_map.idx(nd->_idx));
      }
#endif
      return nnn;
    }
  }
  return 0;
}

int SuperWord::mark_generations() {
  Node *ii_err = 0, *tail_err;
  for (int i = 0; i < _mem_slice_head.length(); i++) {
    Node* phi  = _mem_slice_head.at(i);
    assert(phi->is_Phi(), "must be phi");

    Node* tail = _mem_slice_tail.at(i);
    if (_ii_last == -1) {
      tail_err = tail;
      _ii_last = _clone_map.gen(tail->_idx);
    }
    else if (_ii_last != _clone_map.gen(tail->_idx)) {
#ifndef PRODUCT
      if (TraceSuperWord && Verbose) {
        tty->print_cr("SuperWord::mark_generations _ii_last error - found different generations in two tail nodes ");
        tail->dump();
        tail_err->dump();
      }
#endif
      return -1;
    }

    // find first iteration in the loop
    for (DUIterator_Fast imax, i = phi->fast_outs(imax); i < imax; i++) {
      Node* ii = phi->fast_out(i);
      if (in_bb(ii) && ii->is_Store()) { // we speculate that normally Stores of one and one only generation have deps from mem phi
        if (_ii_first == -1) {
          ii_err = ii;
          _ii_first = _clone_map.gen(ii->_idx);
        } else if (_ii_first != _clone_map.gen(ii->_idx)) {
#ifndef PRODUCT
          if (TraceSuperWord && Verbose) {
            tty->print_cr("SuperWord::mark_generations _ii_first error - found different generations in two nodes ");
            ii->dump();
            ii_err->dump();
          }
#endif
          return -1; // this phi has Stores from different generations of unroll and cannot be simd/vectorized
        }
      }
    }//for (DUIterator_Fast imax,
  }//for (int i...

  if (_ii_first == -1 || _ii_last == -1) {
#ifndef PRODUCT
    if (TraceSuperWord && Verbose) {
      tty->print_cr("SuperWord::mark_generations unknown error, something vent wrong");
    }
#endif
    return -1; // something vent wrong
  }
  // collect nodes in the first and last generations
  assert(_iteration_first.length() == 0, "_iteration_first must be empty");
  assert(_iteration_last.length() == 0, "_iteration_last must be empty");
  for (int j = 0; j < _block.length(); j++) {
    Node* n = _block.at(j);
    node_idx_t gen = _clone_map.gen(n->_idx);
    if ((signed)gen == _ii_first) {
      _iteration_first.push(n);
    } else if ((signed)gen == _ii_last) {
      _iteration_last.push(n);
    }
  }

  // building order of iterations
  assert(_ii_order.length() == 0, "should be empty");
  if (ii_err != 0) {
    assert(in_bb(ii_err) && ii_err->is_Store(), "should be Store in bb");
    Node* nd = ii_err;
    while(_clone_map.gen(nd->_idx) != _ii_last) {
      _ii_order.push(_clone_map.gen(nd->_idx));
      bool found = false;
      for (DUIterator_Fast imax, i = nd->fast_outs(imax); i < imax; i++) {
        Node* use = nd->fast_out(i);
        if (_clone_map.idx(use->_idx) == _clone_map.idx(nd->_idx) && use->as_Store()->in(MemNode::Memory) == nd) {
          found = true;
          nd = use;
          break;
        }
      }//for

      if (found == false) {
#ifndef PRODUCT
        if (TraceSuperWord && Verbose) {
          tty->print_cr("SuperWord::mark_generations: Cannot build order of iterations - no dependent Store for %d", nd->_idx);
        }
#endif
        _ii_order.clear();
        return -1;
      }
    } //while
    _ii_order.push(_clone_map.gen(nd->_idx));
  }

#ifndef PRODUCT
  if (_vector_loop_debug) {
    tty->print_cr("SuperWord::mark_generations");
    tty->print_cr("First generation (%d) nodes:", _ii_first);
    for (int ii = 0; ii < _iteration_first.length(); ii++)  _iteration_first.at(ii)->dump();
    tty->print_cr("Last generation (%d) nodes:", _ii_last);
    for (int ii = 0; ii < _iteration_last.length(); ii++)  _iteration_last.at(ii)->dump();
    tty->print_cr(" ");

    tty->print("SuperWord::List of generations: ");
    for (int jj = 0; jj < _ii_order.length(); ++jj) {
      tty->print("%d:%d ", jj, _ii_order.at(jj));
    }
    tty->print_cr(" ");
  }
#endif

  return _ii_first;
}

bool SuperWord::fix_commutative_inputs(Node* gold, Node* fix) {
  assert(gold->is_Add() && fix->is_Add() || gold->is_Mul() && fix->is_Mul(), "should be only Add or Mul nodes");
  assert(_clone_map.idx(gold->_idx) == _clone_map.idx(fix->_idx), "should be clones of the same node");
  Node* gin1 = gold->in(1);
  Node* gin2 = gold->in(2);
  Node* fin1 = fix->in(1);
  Node* fin2 = fix->in(2);
  bool swapped = false;

  if (in_bb(gin1) && in_bb(gin2) && in_bb(fin1) && in_bb(fin1)) {
    if (_clone_map.idx(gin1->_idx) == _clone_map.idx(fin1->_idx) &&
        _clone_map.idx(gin2->_idx) == _clone_map.idx(fin2->_idx)) {
      return true; // nothing to fix
    }
    if (_clone_map.idx(gin1->_idx) == _clone_map.idx(fin2->_idx) &&
        _clone_map.idx(gin2->_idx) == _clone_map.idx(fin1->_idx)) {
      fix->swap_edges(1, 2);
      swapped = true;
    }
  }
  // at least one input comes from outside of bb
  if (gin1->_idx == fin1->_idx)  {
    return true; // nothing to fix
  }
  if (!swapped && (gin1->_idx == fin2->_idx || gin2->_idx == fin1->_idx))  { //swapping is expensive, check condition first
    fix->swap_edges(1, 2);
    swapped = true;
  }

  if (swapped) {
#ifndef PRODUCT
    if (_vector_loop_debug) {
      tty->print_cr("SuperWord::fix_commutative_inputs: fixed node %d", fix->_idx);
    }
#endif
    return true;
  }

#ifndef PRODUCT
  if (TraceSuperWord && Verbose) {
    tty->print_cr("SuperWord::fix_commutative_inputs: cannot fix node %d", fix->_idx);
  }
#endif
  return false;
}

bool SuperWord::pack_parallel() {
#ifndef PRODUCT
  if (_vector_loop_debug) {
    tty->print_cr("SuperWord::pack_parallel: START");
  }
#endif

  _packset.clear();

  for (int ii = 0; ii < _iteration_first.length(); ii++) {
    Node* nd = _iteration_first.at(ii);
    if (in_bb(nd) && (nd->is_Load() || nd->is_Store() || nd->is_Add() || nd->is_Mul())) {
      Node_List* pk = new Node_List();
      pk->push(nd);
      for (int gen = 1; gen < _ii_order.length(); ++gen) {
        for (int kk = 0; kk < _block.length(); kk++) {
          Node* clone = _block.at(kk);
          if (_clone_map.idx(clone->_idx) == _clone_map.idx(nd->_idx) &&
              _clone_map.gen(clone->_idx) == _ii_order.at(gen)) {
            if (nd->is_Add() || nd->is_Mul()) {
              fix_commutative_inputs(nd, clone);
            }
            pk->push(clone);
            if (pk->size() == 4) {
              _packset.append(pk);
#ifndef PRODUCT
              if (_vector_loop_debug) {
                tty->print_cr("SuperWord::pack_parallel: added pack ");
                pk->dump();
              }
#endif
              if (_clone_map.gen(clone->_idx) != _ii_last) {
                pk = new Node_List();
              }
            }
            break;
          }
        }
      }//for
    }//if
  }//for

#ifndef PRODUCT
  if (_vector_loop_debug) {
    tty->print_cr("SuperWord::pack_parallel: END");
  }
#endif

  return true;
}

bool SuperWord::hoist_loads_in_graph() {
  GrowableArray<Node*> loads;

#ifndef PRODUCT
  if (_vector_loop_debug) {
    tty->print_cr("SuperWord::hoist_loads_in_graph: total number _mem_slice_head.length() = %d", _mem_slice_head.length());
  }
#endif

  for (int i = 0; i < _mem_slice_head.length(); i++) {
    Node* n = _mem_slice_head.at(i);
    if ( !in_bb(n) || !n->is_Phi() || n->bottom_type() != Type::MEMORY) {
#ifndef PRODUCT
      if (TraceSuperWord && Verbose) {
        tty->print_cr("SuperWord::hoist_loads_in_graph: skipping unexpected node n=%d", n->_idx);
      }
#endif
      continue;
    }

#ifndef PRODUCT
    if (_vector_loop_debug) {
      tty->print_cr("SuperWord::hoist_loads_in_graph: processing phi %d  = _mem_slice_head.at(%d);", n->_idx, i);
    }
#endif

    for (DUIterator_Fast imax, i = n->fast_outs(imax); i < imax; i++) {
      Node* ld = n->fast_out(i);
      if (ld->is_Load() && ld->as_Load()->in(MemNode::Memory) == n && in_bb(ld)) {
        for (int i = 0; i < _block.length(); i++) {
          Node* ld2 = _block.at(i);
          if (ld2->is_Load() &&
              _clone_map.idx(ld->_idx) == _clone_map.idx(ld2->_idx) &&
              _clone_map.gen(ld->_idx) != _clone_map.gen(ld2->_idx)) { // <= do not collect the first generation ld
#ifndef PRODUCT
            if (_vector_loop_debug) {
              tty->print_cr("SuperWord::hoist_loads_in_graph: will try to hoist load ld2->_idx=%d, cloned from %d (ld->_idx=%d)",
                            ld2->_idx, _clone_map.idx(ld->_idx), ld->_idx);
            }
#endif
            // could not do on-the-fly, since iterator is immutable
            loads.push(ld2);
          }
        }// for
      }//if
    }//for (DUIterator_Fast imax,
  }//for (int i = 0; i

  for (int i = 0; i < loads.length(); i++) {
    LoadNode* ld = loads.at(i)->as_Load();
    Node* phi = find_phi_for_mem_dep(ld);
    if (phi != NULL) {
#ifndef PRODUCT
      if (_vector_loop_debug) {
        tty->print_cr("SuperWord::hoist_loads_in_graph replacing MemNode::Memory(%d) edge in %d with one from %d",
                      MemNode::Memory, ld->_idx, phi->_idx);
      }
#endif
      _igvn.replace_input_of(ld, MemNode::Memory, phi);
    }
  }//for

  restart(); // invalidate all basic structures, since we rebuilt the graph

#ifndef PRODUCT
  if (TraceSuperWord && Verbose) {
    tty->print_cr("\nSuperWord::hoist_loads_in_graph() the graph was rebuilt, all structures invalidated and need rebuild");
  }
#endif
  return true;
}

