/*
 * Copyright (c) 1998, 2012, Oracle and/or its affiliates. All rights reserved.
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
#include "ci/ciReplay.hpp"
#include "classfile/systemDictionary.hpp"
#include "classfile/vmSymbols.hpp"
#include "compiler/compileBroker.hpp"
#include "compiler/compileLog.hpp"
#include "interpreter/linkResolver.hpp"
#include "oops/objArrayKlass.hpp"
#include "opto/callGenerator.hpp"
#include "opto/parse.hpp"
#include "runtime/handles.inline.hpp"

//=============================================================================
//------------------------------InlineTree-------------------------------------
InlineTree::InlineTree(Compile* c,
                       const InlineTree *caller_tree, ciMethod* callee,
                       JVMState* caller_jvms, int caller_bci,
                       float site_invoke_ratio, int max_inline_level) :
  C(c),
  _caller_jvms(caller_jvms),
  _caller_tree((InlineTree*) caller_tree),
  _method(callee),
  _site_invoke_ratio(site_invoke_ratio),
  _max_inline_level(max_inline_level),
  _count_inline_bcs(method()->code_size_for_inlining()),
  _subtrees(c->comp_arena(), 2, 0, NULL),
  _msg(NULL)
{
  NOT_PRODUCT(_count_inlines = 0;)
  if (_caller_jvms != NULL) {
    // Keep a private copy of the caller_jvms:
    _caller_jvms = new (C) JVMState(caller_jvms->method(), caller_tree->caller_jvms());
    _caller_jvms->set_bci(caller_jvms->bci());
    assert(!caller_jvms->should_reexecute(), "there should be no reexecute bytecode with inlining");
  }
  assert(_caller_jvms->same_calls_as(caller_jvms), "consistent JVMS");
  assert((caller_tree == NULL ? 0 : caller_tree->stack_depth() + 1) == stack_depth(), "correct (redundant) depth parameter");
  assert(caller_bci == this->caller_bci(), "correct (redundant) bci parameter");
  if (UseOldInlining) {
    // Update hierarchical counts, count_inline_bcs() and count_inlines()
    InlineTree *caller = (InlineTree *)caller_tree;
    for( ; caller != NULL; caller = ((InlineTree *)(caller->caller_tree())) ) {
      caller->_count_inline_bcs += count_inline_bcs();
      NOT_PRODUCT(caller->_count_inlines++;)
    }
  }
}

InlineTree::InlineTree(Compile* c, ciMethod* callee_method, JVMState* caller_jvms,
                       float site_invoke_ratio, int max_inline_level) :
  C(c),
  _caller_jvms(caller_jvms),
  _caller_tree(NULL),
  _method(callee_method),
  _site_invoke_ratio(site_invoke_ratio),
  _max_inline_level(max_inline_level),
  _count_inline_bcs(method()->code_size()),
  _msg(NULL)
{
  NOT_PRODUCT(_count_inlines = 0;)
  assert(!UseOldInlining, "do not use for old stuff");
}

static bool is_init_with_ea(ciMethod* callee_method,
                            ciMethod* caller_method, Compile* C) {
  // True when EA is ON and a java constructor is called or
  // a super constructor is called from an inlined java constructor.
  return C->do_escape_analysis() && EliminateAllocations &&
         ( callee_method->is_initializer() ||
           (caller_method->is_initializer() &&
            caller_method != C->method() &&
            caller_method->holder()->is_subclass_of(callee_method->holder()))
         );
}

// positive filter: should callee be inlined?
bool InlineTree::should_inline(ciMethod* callee_method, ciMethod* caller_method,
                               int caller_bci, ciCallProfile& profile,
                               WarmCallInfo* wci_result) {
  // Allows targeted inlining
  if(callee_method->should_inline()) {
    *wci_result = *(WarmCallInfo::always_hot());
    if (PrintInlining && Verbose) {
      CompileTask::print_inline_indent(inline_level());
      tty->print_cr("Inlined method is hot: ");
    }
    set_msg("force inline by CompilerOracle");
    return true;
  }

  int size = callee_method->code_size_for_inlining();

  // Check for too many throws (and not too huge)
  if(callee_method->interpreter_throwout_count() > InlineThrowCount &&
     size < InlineThrowMaxSize ) {
    wci_result->set_profit(wci_result->profit() * 100);
    if (PrintInlining && Verbose) {
      CompileTask::print_inline_indent(inline_level());
      tty->print_cr("Inlined method with many throws (throws=%d):", callee_method->interpreter_throwout_count());
    }
    set_msg("many throws");
    return true;
  }

  if (!UseOldInlining) {
    set_msg("!UseOldInlining");
    return true;  // size and frequency are represented in a new way
  }

  int default_max_inline_size = C->max_inline_size();
  int inline_small_code_size  = InlineSmallCode / 4;
  int max_inline_size         = default_max_inline_size;

  int call_site_count  = method()->scale_count(profile.count());
  int invoke_count     = method()->interpreter_invocation_count();

  assert(invoke_count != 0, "require invocation count greater than zero");
  int freq = call_site_count / invoke_count;

  // bump the max size if the call is frequent
  if ((freq >= InlineFrequencyRatio) ||
      (call_site_count >= InlineFrequencyCount) ||
      is_init_with_ea(callee_method, caller_method, C)) {

    max_inline_size = C->freq_inline_size();
    if (size <= max_inline_size && TraceFrequencyInlining) {
      CompileTask::print_inline_indent(inline_level());
      tty->print_cr("Inlined frequent method (freq=%d count=%d):", freq, call_site_count);
      CompileTask::print_inline_indent(inline_level());
      callee_method->print();
      tty->cr();
    }
  } else {
    // Not hot.  Check for medium-sized pre-existing nmethod at cold sites.
    if (callee_method->has_compiled_code() &&
        callee_method->instructions_size() > inline_small_code_size) {
      set_msg("already compiled into a medium method");
      return false;
    }
  }
  if (size > max_inline_size) {
    if (max_inline_size > default_max_inline_size) {
      set_msg("hot method too big");
    } else {
      set_msg("too big");
    }
    return false;
  }
  return true;
}


// negative filter: should callee NOT be inlined?
bool InlineTree::should_not_inline(ciMethod *callee_method,
                                   ciMethod* caller_method,
                                   WarmCallInfo* wci_result) {

  const char* fail_msg = NULL;

  // First check all inlining restrictions which are required for correctness
  if ( callee_method->is_abstract()) {
    fail_msg = "abstract method"; // // note: we allow ik->is_abstract()
  } else if (!callee_method->holder()->is_initialized()) {
    fail_msg = "method holder not initialized";
  } else if ( callee_method->is_native()) {
    fail_msg = "native method";
  } else if ( callee_method->dont_inline()) {
    fail_msg = "don't inline by annotation";
  }

  if (!UseOldInlining) {
    if (fail_msg != NULL) {
      *wci_result = *(WarmCallInfo::always_cold());
      set_msg(fail_msg);
      return true;
    }

    if (callee_method->has_unloaded_classes_in_signature()) {
      wci_result->set_profit(wci_result->profit() * 0.1);
    }

    // don't inline exception code unless the top method belongs to an
    // exception class
    if (callee_method->holder()->is_subclass_of(C->env()->Throwable_klass())) {
      ciMethod* top_method = caller_jvms() ? caller_jvms()->of_depth(1)->method() : method();
      if (!top_method->holder()->is_subclass_of(C->env()->Throwable_klass())) {
        wci_result->set_profit(wci_result->profit() * 0.1);
      }
    }

    if (callee_method->has_compiled_code() &&
        callee_method->instructions_size() > InlineSmallCode) {
      wci_result->set_profit(wci_result->profit() * 0.1);
      // %%% adjust wci_result->size()?
    }

    return false;
  }

  // one more inlining restriction
  if (fail_msg == NULL && callee_method->has_unloaded_classes_in_signature()) {
    fail_msg = "unloaded signature classes";
  }

  if (fail_msg != NULL) {
    set_msg(fail_msg);
    return true;
  }

  // ignore heuristic controls on inlining
  if (callee_method->should_inline()) {
    set_msg("force inline by CompilerOracle");
    return false;
  }

  // Now perform checks which are heuristic

  if (!callee_method->force_inline()) {
    if (callee_method->has_compiled_code() &&
        callee_method->instructions_size() > InlineSmallCode) {
      set_msg("already compiled into a big method");
      return true;
    }
  }

  // don't inline exception code unless the top method belongs to an
  // exception class
  if (caller_tree() != NULL &&
      callee_method->holder()->is_subclass_of(C->env()->Throwable_klass())) {
    const InlineTree *top = this;
    while (top->caller_tree() != NULL) top = top->caller_tree();
    ciInstanceKlass* k = top->method()->holder();
    if (!k->is_subclass_of(C->env()->Throwable_klass())) {
      set_msg("exception method");
      return true;
    }
  }

  if (callee_method->should_not_inline()) {
    set_msg("disallowed by CompilerOracle");
    return true;
  }

#ifndef PRODUCT
  if (ciReplay::should_not_inline(callee_method)) {
    set_msg("disallowed by ciReplay");
    return true;
  }
#endif

  if (UseStringCache) {
    // Do not inline StringCache::profile() method used only at the beginning.
    if (callee_method->name() == ciSymbol::profile_name() &&
        callee_method->holder()->name() == ciSymbol::java_lang_StringCache()) {
      set_msg("profiling method");
      return true;
    }
  }

  // use frequency-based objections only for non-trivial methods
  if (callee_method->code_size() <= MaxTrivialSize) {
    return false;
  }

  // don't use counts with -Xcomp or CTW
  if (UseInterpreter && !CompileTheWorld) {

    if (!callee_method->has_compiled_code() &&
        !callee_method->was_executed_more_than(0)) {
      set_msg("never executed");
      return true;
    }

    if (is_init_with_ea(callee_method, caller_method, C)) {

      // Escape Analysis: inline all executed constructors

    } else if (!callee_method->was_executed_more_than(MIN2(MinInliningThreshold,
                                                           CompileThreshold >> 1))) {
      set_msg("executed < MinInliningThreshold times");
      return true;
    }
  }

  return false;
}

//-----------------------------try_to_inline-----------------------------------
// return true if ok
// Relocated from "InliningClosure::try_to_inline"
bool InlineTree::try_to_inline(ciMethod* callee_method, ciMethod* caller_method,
                               int caller_bci, ciCallProfile& profile,
                               WarmCallInfo* wci_result, bool& should_delay) {

   // Old algorithm had funny accumulating BC-size counters
  if (UseOldInlining && ClipInlining
      && (int)count_inline_bcs() >= DesiredMethodLimit) {
    if (!callee_method->force_inline() || !IncrementalInline) {
      set_msg("size > DesiredMethodLimit");
      return false;
    } else if (!C->inlining_incrementally()) {
      should_delay = true;
    }
  }

  if (!should_inline(callee_method, caller_method, caller_bci, profile,
                     wci_result)) {
    return false;
  }
  if (should_not_inline(callee_method, caller_method, wci_result)) {
    return false;
  }

  if (InlineAccessors && callee_method->is_accessor()) {
    // accessor methods are not subject to any of the following limits.
    set_msg("accessor");
    return true;
  }

  // suppress a few checks for accessors and trivial methods
  if (callee_method->code_size() > MaxTrivialSize) {

    // don't inline into giant methods
    if (C->over_inlining_cutoff()) {
      if ((!callee_method->force_inline() && !caller_method->is_compiled_lambda_form())
          || !IncrementalInline) {
        set_msg("NodeCountInliningCutoff");
        return false;
      } else {
        should_delay = true;
      }
    }

    if ((!UseInterpreter || CompileTheWorld) &&
        is_init_with_ea(callee_method, caller_method, C)) {

      // Escape Analysis stress testing when running Xcomp or CTW:
      // inline constructors even if they are not reached.

    } else if (profile.count() == 0) {
      // don't inline unreached call sites
       set_msg("call site not reached");
       return false;
    }
  }

  if (!C->do_inlining() && InlineAccessors) {
    set_msg("not an accessor");
    return false;
  }
  if (inline_level() > _max_inline_level) {
    if (!callee_method->force_inline() || !IncrementalInline) {
      set_msg("inlining too deep");
      return false;
    } else if (!C->inlining_incrementally()) {
      should_delay = true;
    }
  }

  // detect direct and indirect recursive inlining
  if (!callee_method->is_compiled_lambda_form()) {
    // count the current method and the callee
    int inline_level = (method() == callee_method) ? 1 : 0;
    if (inline_level > MaxRecursiveInlineLevel) {
      set_msg("recursively inlining too deep");
      return false;
    }
    // count callers of current method and callee
    JVMState* jvms = caller_jvms();
    while (jvms != NULL && jvms->has_method()) {
      if (jvms->method() == callee_method) {
        inline_level++;
        if (inline_level > MaxRecursiveInlineLevel) {
          set_msg("recursively inlining too deep");
          return false;
        }
      }
      jvms = jvms->caller();
    }
  }

  int size = callee_method->code_size_for_inlining();

  if (UseOldInlining && ClipInlining
      && (int)count_inline_bcs() + size >= DesiredMethodLimit) {
    if (!callee_method->force_inline() || !IncrementalInline) {
      set_msg("size > DesiredMethodLimit");
      return false;
    } else if (!C->inlining_incrementally()) {
      should_delay = true;
    }
  }

  // ok, inline this method
  return true;
}

//------------------------------pass_initial_checks----------------------------
bool pass_initial_checks(ciMethod* caller_method, int caller_bci, ciMethod* callee_method) {
  ciInstanceKlass *callee_holder = callee_method ? callee_method->holder() : NULL;
  // Check if a callee_method was suggested
  if( callee_method == NULL )            return false;
  // Check if klass of callee_method is loaded
  if( !callee_holder->is_loaded() )      return false;
  if( !callee_holder->is_initialized() ) return false;
  if( !UseInterpreter || CompileTheWorld /* running Xcomp or CTW */ ) {
    // Checks that constant pool's call site has been visited
    // stricter than callee_holder->is_initialized()
    ciBytecodeStream iter(caller_method);
    iter.force_bci(caller_bci);
    Bytecodes::Code call_bc = iter.cur_bc();
    // An invokedynamic instruction does not have a klass.
    if (call_bc != Bytecodes::_invokedynamic) {
      int index = iter.get_index_u2_cpcache();
      if (!caller_method->is_klass_loaded(index, true)) {
        return false;
      }
      // Try to do constant pool resolution if running Xcomp
      if( !caller_method->check_call(index, call_bc == Bytecodes::_invokestatic) ) {
        return false;
      }
    }
  }
  // We will attempt to see if a class/field/etc got properly loaded.  If it
  // did not, it may attempt to throw an exception during our probing.  Catch
  // and ignore such exceptions and do not attempt to compile the method.
  if( callee_method->should_exclude() )  return false;

  return true;
}

//------------------------------check_can_parse--------------------------------
const char* InlineTree::check_can_parse(ciMethod* callee) {
  // Certain methods cannot be parsed at all:
  if ( callee->is_native())                     return "native method";
  if ( callee->is_abstract())                   return "abstract method";
  if (!callee->can_be_compiled())               return "not compilable (disabled)";
  if (!callee->has_balanced_monitors())         return "not compilable (unbalanced monitors)";
  if ( callee->get_flow_analysis()->failing())  return "not compilable (flow analysis failed)";
  return NULL;
}

//------------------------------print_inlining---------------------------------
void InlineTree::print_inlining(ciMethod* callee_method, int caller_bci,
                                bool success) const {
  const char* inline_msg = msg();
  assert(inline_msg != NULL, "just checking");
  if (C->log() != NULL) {
    if (success) {
      C->log()->inline_success(inline_msg);
    } else {
      C->log()->inline_fail(inline_msg);
    }
  }
  if (PrintInlining) {
    C->print_inlining(callee_method, inline_level(), caller_bci, inline_msg);
    if (callee_method == NULL) tty->print(" callee not monotonic or profiled");
    if (Verbose && callee_method) {
      const InlineTree *top = this;
      while( top->caller_tree() != NULL ) { top = top->caller_tree(); }
      //tty->print("  bcs: %d+%d  invoked: %d", top->count_inline_bcs(), callee_method->code_size(), callee_method->interpreter_invocation_count());
    }
  }
}

//------------------------------ok_to_inline-----------------------------------
WarmCallInfo* InlineTree::ok_to_inline(ciMethod* callee_method, JVMState* jvms, ciCallProfile& profile, WarmCallInfo* initial_wci, bool& should_delay) {
  assert(callee_method != NULL, "caller checks for optimized virtual!");
  assert(!should_delay, "should be initialized to false");
#ifdef ASSERT
  // Make sure the incoming jvms has the same information content as me.
  // This means that we can eventually make this whole class AllStatic.
  if (jvms->caller() == NULL) {
    assert(_caller_jvms == NULL, "redundant instance state");
  } else {
    assert(_caller_jvms->same_calls_as(jvms->caller()), "redundant instance state");
  }
  assert(_method == jvms->method(), "redundant instance state");
#endif
  int         caller_bci    = jvms->bci();
  ciMethod*   caller_method = jvms->method();

  // Do some initial checks.
  if (!pass_initial_checks(caller_method, caller_bci, callee_method)) {
    set_msg("failed initial checks");
    print_inlining(callee_method, caller_bci, false /* !success */);
    return NULL;
  }

  // Do some parse checks.
  set_msg(check_can_parse(callee_method));
  if (msg() != NULL) {
    print_inlining(callee_method, caller_bci, false /* !success */);
    return NULL;
  }

  // Check if inlining policy says no.
  WarmCallInfo wci = *(initial_wci);
  bool success = try_to_inline(callee_method, caller_method, caller_bci,
                               profile, &wci, should_delay);

#ifndef PRODUCT
  if (UseOldInlining && InlineWarmCalls
      && (PrintOpto || PrintOptoInlining || PrintInlining)) {
    bool cold = wci.is_cold();
    bool hot  = !cold && wci.is_hot();
    bool old_cold = !success;
    if (old_cold != cold || (Verbose || WizardMode)) {
      if (msg() == NULL) {
        set_msg("OK");
      }
      tty->print("   OldInlining= %4s : %s\n           WCI=",
                 old_cold ? "cold" : "hot", msg());
      wci.print();
    }
  }
#endif
  if (UseOldInlining) {
    if (success) {
      wci = *(WarmCallInfo::always_hot());
    } else {
      wci = *(WarmCallInfo::always_cold());
    }
  }
  if (!InlineWarmCalls) {
    if (!wci.is_cold() && !wci.is_hot()) {
      // Do not inline the warm calls.
      wci = *(WarmCallInfo::always_cold());
    }
  }

  if (!wci.is_cold()) {
    // Inline!
    if (msg() == NULL) {
      set_msg("inline (hot)");
    }
    print_inlining(callee_method, caller_bci, true /* success */);
    if (UseOldInlining)
      build_inline_tree_for_callee(callee_method, jvms, caller_bci);
    if (InlineWarmCalls && !wci.is_hot())
      return new (C) WarmCallInfo(wci);  // copy to heap
    return WarmCallInfo::always_hot();
  }

  // Do not inline
  if (msg() == NULL) {
    set_msg("too cold to inline");
  }
  print_inlining(callee_method, caller_bci, false /* !success */ );
  return NULL;
}

//------------------------------compute_callee_frequency-----------------------
float InlineTree::compute_callee_frequency( int caller_bci ) const {
  int count  = method()->interpreter_call_site_count(caller_bci);
  int invcnt = method()->interpreter_invocation_count();
  float freq = (float)count/(float)invcnt;
  // Call-site count / interpreter invocation count, scaled recursively.
  // Always between 0.0 and 1.0.  Represents the percentage of the method's
  // total execution time used at this call site.

  return freq;
}

//------------------------------build_inline_tree_for_callee-------------------
InlineTree *InlineTree::build_inline_tree_for_callee( ciMethod* callee_method, JVMState* caller_jvms, int caller_bci) {
  float recur_frequency = _site_invoke_ratio * compute_callee_frequency(caller_bci);
  // Attempt inlining.
  InlineTree* old_ilt = callee_at(caller_bci, callee_method);
  if (old_ilt != NULL) {
    return old_ilt;
  }
  int max_inline_level_adjust = 0;
  if (caller_jvms->method() != NULL) {
    if (caller_jvms->method()->is_compiled_lambda_form())
      max_inline_level_adjust += 1;  // don't count actions in MH or indy adapter frames
    else if (callee_method->is_method_handle_intrinsic() ||
             callee_method->is_compiled_lambda_form()) {
      max_inline_level_adjust += 1;  // don't count method handle calls from java.lang.invoke implem
    }
    if (max_inline_level_adjust != 0 && PrintInlining && (Verbose || WizardMode)) {
      CompileTask::print_inline_indent(inline_level());
      tty->print_cr(" \\-> discounting inline depth");
    }
    if (max_inline_level_adjust != 0 && C->log()) {
      int id1 = C->log()->identify(caller_jvms->method());
      int id2 = C->log()->identify(callee_method);
      C->log()->elem("inline_level_discount caller='%d' callee='%d'", id1, id2);
    }
  }
  InlineTree* ilt = new InlineTree(C, this, callee_method, caller_jvms, caller_bci, recur_frequency, _max_inline_level + max_inline_level_adjust);
  _subtrees.append(ilt);

  NOT_PRODUCT( _count_inlines += 1; )

  return ilt;
}


//---------------------------------------callee_at-----------------------------
InlineTree *InlineTree::callee_at(int bci, ciMethod* callee) const {
  for (int i = 0; i < _subtrees.length(); i++) {
    InlineTree* sub = _subtrees.at(i);
    if (sub->caller_bci() == bci && callee == sub->method()) {
      return sub;
    }
  }
  return NULL;
}


//------------------------------build_inline_tree_root-------------------------
InlineTree *InlineTree::build_inline_tree_root() {
  Compile* C = Compile::current();

  // Root of inline tree
  InlineTree* ilt = new InlineTree(C, NULL, C->method(), NULL, -1, 1.0F, MaxInlineLevel);

  return ilt;
}


//-------------------------find_subtree_from_root-----------------------------
// Given a jvms, which determines a call chain from the root method,
// find the corresponding inline tree.
// Note: This method will be removed or replaced as InlineTree goes away.
InlineTree* InlineTree::find_subtree_from_root(InlineTree* root, JVMState* jvms, ciMethod* callee) {
  InlineTree* iltp = root;
  uint depth = jvms && jvms->has_method() ? jvms->depth() : 0;
  for (uint d = 1; d <= depth; d++) {
    JVMState* jvmsp  = jvms->of_depth(d);
    // Select the corresponding subtree for this bci.
    assert(jvmsp->method() == iltp->method(), "tree still in sync");
    ciMethod* d_callee = (d == depth) ? callee : jvms->of_depth(d+1)->method();
    InlineTree* sub = iltp->callee_at(jvmsp->bci(), d_callee);
    if (sub == NULL) {
      if (d == depth) {
        sub = iltp->build_inline_tree_for_callee(d_callee, jvmsp, jvmsp->bci());
      }
      guarantee(sub != NULL, "should be a sub-ilt here");
      return sub;
    }
    iltp = sub;
  }
  return iltp;
}



#ifndef PRODUCT
void InlineTree::print_impl(outputStream* st, int indent) const {
  for (int i = 0; i < indent; i++) st->print(" ");
  st->print(" @ %d ", caller_bci());
  method()->print_short_name(st);
  st->cr();

  for (int i = 0 ; i < _subtrees.length(); i++) {
    _subtrees.at(i)->print_impl(st, indent + 2);
  }
}

void InlineTree::print_value_on(outputStream* st) const {
  print_impl(st, 2);
}
#endif
