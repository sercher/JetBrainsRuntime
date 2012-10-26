/*
 * Copyright (c) 1997, 2012, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_VM_RUNTIME_SIGNATURE_HPP
#define SHARE_VM_RUNTIME_SIGNATURE_HPP

#include "memory/allocation.hpp"
#include "oops/method.hpp"
#include "utilities/top.hpp"

// SignatureIterators iterate over a Java signature (or parts of it).
// (Syntax according to: "The Java Virtual Machine Specification" by
// Tim Lindholm & Frank Yellin; section 4.3 Descriptors; p. 89ff.)
//
// Example: Iterating over ([Lfoo;D)I using
//                         0123456789
//
// iterate_parameters() calls: do_array(2, 7); do_double();
// iterate_returntype() calls:                              do_int();
// iterate()            calls: do_array(2, 7); do_double(); do_int();
//
// is_return_type()        is: false         ; false      ; true
//
// NOTE: The new optimizer has an alternate, for-loop based signature
// iterator implemented in opto/type.cpp, TypeTuple::make().

class SignatureIterator: public ResourceObj {
 protected:
  Symbol*      _signature;             // the signature to iterate over
  int          _index;                 // the current character index (only valid during iteration)
  int          _parameter_index;       // the current parameter index (0 outside iteration phase)
  BasicType    _return_type;

  void expect(char c);
  void skip_optional_size();
  int  parse_type();                   // returns the parameter size in words (0 for void)
  void check_signature_end();

 public:
  // Definitions used in generating and iterating the
  // bit field form of the signature generated by the
  // Fingerprinter.
  enum {
    static_feature_size    = 1,
    result_feature_size    = 4,
    result_feature_mask    = 0xF,
    parameter_feature_size = 4,
    parameter_feature_mask = 0xF,

      bool_parm            = 1,
      byte_parm            = 2,
      char_parm            = 3,
      short_parm           = 4,
      int_parm             = 5,
      long_parm            = 6,
      float_parm           = 7,
      double_parm          = 8,
      obj_parm             = 9,
      done_parm            = 10,  // marker for end of parameters

    // max parameters is wordsize minus
    //    The sign bit, termination field, the result and static bit fields
    max_size_of_parameters = (BitsPerLong-1 -
                              result_feature_size - parameter_feature_size -
                              static_feature_size) / parameter_feature_size
  };

  // Constructors
  SignatureIterator(Symbol* signature);

  // Iteration
  void dispatch_field();               // dispatches once for field signatures
  void iterate_parameters();           // iterates over parameters only
  void iterate_parameters( uint64_t fingerprint );
  void iterate_returntype();           // iterates over returntype only
  void iterate();                      // iterates over whole signature
  // Returns the word index of the current parameter;
  int  parameter_index() const         { return _parameter_index; }
  bool is_return_type() const          { return parameter_index() < 0; }
  BasicType get_ret_type() const       { return _return_type; }

  // Basic types
  virtual void do_bool  ()             = 0;
  virtual void do_char  ()             = 0;
  virtual void do_float ()             = 0;
  virtual void do_double()             = 0;
  virtual void do_byte  ()             = 0;
  virtual void do_short ()             = 0;
  virtual void do_int   ()             = 0;
  virtual void do_long  ()             = 0;
  virtual void do_void  ()             = 0;

  // Object types (begin indexes the first character of the entry, end indexes the first character after the entry)
  virtual void do_object(int begin, int end) = 0;
  virtual void do_array (int begin, int end) = 0;
};


// Specialized SignatureIterators: Used to compute signature specific values.

class SignatureTypeNames : public SignatureIterator {
 protected:
  virtual void type_name(const char* name)   = 0;

  void do_bool()                       { type_name("jboolean"); }
  void do_char()                       { type_name("jchar"   ); }
  void do_float()                      { type_name("jfloat"  ); }
  void do_double()                     { type_name("jdouble" ); }
  void do_byte()                       { type_name("jbyte"   ); }
  void do_short()                      { type_name("jshort"  ); }
  void do_int()                        { type_name("jint"    ); }
  void do_long()                       { type_name("jlong"   ); }
  void do_void()                       { type_name("void"    ); }
  void do_object(int begin, int end)   { type_name("jobject" ); }
  void do_array (int begin, int end)   { type_name("jobject" ); }

 public:
  SignatureTypeNames(Symbol* signature) : SignatureIterator(signature) {}
};


class SignatureInfo: public SignatureIterator {
 protected:
  bool      _has_iterated;             // need this because iterate cannot be called in constructor (set is virtual!)
  bool      _has_iterated_return;
  int       _size;

  void lazy_iterate_parameters()       { if (!_has_iterated) { iterate_parameters(); _has_iterated = true; } }
  void lazy_iterate_return()           { if (!_has_iterated_return) { iterate_returntype(); _has_iterated_return = true; } }

  virtual void set(int size, BasicType type) = 0;

  void do_bool  ()                     { set(T_BOOLEAN_size, T_BOOLEAN); }
  void do_char  ()                     { set(T_CHAR_size   , T_CHAR   ); }
  void do_float ()                     { set(T_FLOAT_size  , T_FLOAT  ); }
  void do_double()                     { set(T_DOUBLE_size , T_DOUBLE ); }
  void do_byte  ()                     { set(T_BYTE_size   , T_BYTE   ); }
  void do_short ()                     { set(T_SHORT_size  , T_SHORT  ); }
  void do_int   ()                     { set(T_INT_size    , T_INT    ); }
  void do_long  ()                     { set(T_LONG_size   , T_LONG   ); }
  void do_void  ()                     { set(T_VOID_size   , T_VOID   ); }
  void do_object(int begin, int end)   { set(T_OBJECT_size , T_OBJECT ); }
  void do_array (int begin, int end)   { set(T_ARRAY_size  , T_ARRAY  ); }

 public:
  SignatureInfo(Symbol* signature) : SignatureIterator(signature) {
    _has_iterated = _has_iterated_return = false;
    _size         = 0;
    _return_type  = T_ILLEGAL;
  }

};


// Specialized SignatureIterator: Used to compute the argument size.

class ArgumentSizeComputer: public SignatureInfo {
 private:
  void set(int size, BasicType type)   { _size += size; }
 public:
  ArgumentSizeComputer(Symbol* signature) : SignatureInfo(signature) {}

  int       size()                     { lazy_iterate_parameters(); return _size; }
};


class ArgumentCount: public SignatureInfo {
 private:
  void set(int size, BasicType type)   { _size ++; }
 public:
  ArgumentCount(Symbol* signature) : SignatureInfo(signature) {}

  int       size()                     { lazy_iterate_parameters(); return _size; }
};


// Specialized SignatureIterator: Used to compute the result type.

class ResultTypeFinder: public SignatureInfo {
 private:
  void set(int size, BasicType type)   { _return_type = type; }
 public:
  BasicType type()                     { lazy_iterate_return(); return _return_type; }

  ResultTypeFinder(Symbol* signature) : SignatureInfo(signature) {}
};


// Fingerprinter computes a unique ID for a given method. The ID
// is a bitvector characterizing the methods signature (incl. the receiver).
class Fingerprinter: public SignatureIterator {
 private:
  uint64_t _fingerprint;
  int _shift_count;
  methodHandle mh;

 public:

  void do_bool()    { _fingerprint |= (((uint64_t)bool_parm) << _shift_count); _shift_count += parameter_feature_size; }
  void do_char()    { _fingerprint |= (((uint64_t)char_parm) << _shift_count); _shift_count += parameter_feature_size; }
  void do_byte()    { _fingerprint |= (((uint64_t)byte_parm) << _shift_count); _shift_count += parameter_feature_size; }
  void do_short()   { _fingerprint |= (((uint64_t)short_parm) << _shift_count); _shift_count += parameter_feature_size; }
  void do_int()     { _fingerprint |= (((uint64_t)int_parm) << _shift_count); _shift_count += parameter_feature_size; }
  void do_long()    { _fingerprint |= (((uint64_t)long_parm) << _shift_count); _shift_count += parameter_feature_size; }
  void do_float()   { _fingerprint |= (((uint64_t)float_parm) << _shift_count); _shift_count += parameter_feature_size; }
  void do_double()  { _fingerprint |= (((uint64_t)double_parm) << _shift_count); _shift_count += parameter_feature_size; }

  void do_object(int begin, int end)  { _fingerprint |= (((uint64_t)obj_parm) << _shift_count); _shift_count += parameter_feature_size; }
  void do_array (int begin, int end)  { _fingerprint |= (((uint64_t)obj_parm) << _shift_count); _shift_count += parameter_feature_size; }

  void do_void()    { ShouldNotReachHere(); }

  Fingerprinter(methodHandle method) : SignatureIterator(method->signature()) {
    mh = method;
    _fingerprint = 0;
  }

  uint64_t fingerprint() {
    // See if we fingerprinted this method already
    if (mh->constMethod()->fingerprint() != CONST64(0)) {
      return mh->constMethod()->fingerprint();
    }

    if (mh->size_of_parameters() > max_size_of_parameters ) {
      _fingerprint = UCONST64(-1);
      mh->constMethod()->set_fingerprint(_fingerprint);
      return _fingerprint;
    }

    assert( (int)mh->result_type() <= (int)result_feature_mask, "bad result type");
    _fingerprint = mh->result_type();
    _fingerprint <<= static_feature_size;
    if (mh->is_static())  _fingerprint |= 1;
    _shift_count = result_feature_size + static_feature_size;
    iterate_parameters();
    _fingerprint |= ((uint64_t)done_parm) << _shift_count;// mark end of sig
    mh->constMethod()->set_fingerprint(_fingerprint);
    return _fingerprint;
  }
};


// Specialized SignatureIterator: Used for native call purposes

class NativeSignatureIterator: public SignatureIterator {
 private:
  methodHandle _method;
// We need separate JNI and Java offset values because in 64 bit mode,
// the argument offsets are not in sync with the Java stack.
// For example a long takes up 1 "C" stack entry but 2 Java stack entries.
  int          _offset;                // The java stack offset
  int          _prepended;             // number of prepended JNI parameters (1 JNIEnv, plus 1 mirror if static)
  int          _jni_offset;            // the current parameter offset, starting with 0

  void do_bool  ()                     { pass_int();    _jni_offset++; _offset++;       }
  void do_char  ()                     { pass_int();    _jni_offset++; _offset++;       }
  void do_float ()                     { pass_float();  _jni_offset++; _offset++;       }
#ifdef _LP64
  void do_double()                     { pass_double(); _jni_offset++; _offset += 2;    }
#else
  void do_double()                     { pass_double(); _jni_offset += 2; _offset += 2; }
#endif
  void do_byte  ()                     { pass_int();    _jni_offset++; _offset++;       }
  void do_short ()                     { pass_int();    _jni_offset++; _offset++;       }
  void do_int   ()                     { pass_int();    _jni_offset++; _offset++;       }
#ifdef _LP64
  void do_long  ()                     { pass_long();   _jni_offset++; _offset += 2;    }
#else
  void do_long  ()                     { pass_long();   _jni_offset += 2; _offset += 2; }
#endif
  void do_void  ()                     { ShouldNotReachHere();                               }
  void do_object(int begin, int end)   { pass_object(); _jni_offset++; _offset++;        }
  void do_array (int begin, int end)   { pass_object(); _jni_offset++; _offset++;        }

 public:
  methodHandle method() const          { return _method; }
  int          offset() const          { return _offset; }
  int      jni_offset() const          { return _jni_offset + _prepended; }
//  int     java_offset() const          { return method()->size_of_parameters() - _offset - 1; }
  bool      is_static() const          { return method()->is_static(); }
  virtual void pass_int()              = 0;
  virtual void pass_long()             = 0;
  virtual void pass_object()           = 0;
  virtual void pass_float()            = 0;
#ifdef _LP64
  virtual void pass_double()           = 0;
#else
  virtual void pass_double()           { pass_long(); }  // may be same as long
#endif

  NativeSignatureIterator(methodHandle method) : SignatureIterator(method->signature()) {
    _method = method;
    _offset = 0;
    _jni_offset = 0;

    const int JNIEnv_words = 1;
    const int mirror_words = 1;
    _prepended = !is_static() ? JNIEnv_words : JNIEnv_words + mirror_words;
  }

  // iterate() calles the 2 virtual methods according to the following invocation syntax:
  //
  // {pass_int | pass_long | pass_object}
  //
  // Arguments are handled from left to right (receiver first, if any).
  // The offset() values refer to the Java stack offsets but are 0 based and increasing.
  // The java_offset() values count down to 0, and refer to the Java TOS.
  // The jni_offset() values increase from 1 or 2, and refer to C arguments.

  void iterate() { iterate(Fingerprinter(method()).fingerprint());
  }


  // Optimized path if we have the bitvector form of signature
  void iterate( uint64_t fingerprint ) {

    if (!is_static()) {
      // handle receiver (not handled by iterate because not in signature)
      pass_object(); _jni_offset++; _offset++;
    }

    SignatureIterator::iterate_parameters( fingerprint );
  }
};


// Handy stream for iterating over signature

class SignatureStream : public StackObj {
 private:
  Symbol*      _signature;
  int          _begin;
  int          _end;
  BasicType    _type;
  bool         _at_return_type;
  GrowableArray<Symbol*>* _names;  // symbols created while parsing signature

 public:
  bool at_return_type() const                    { return _at_return_type; }
  bool is_done() const;
  void next_non_primitive(int t);
  void next() {
    Symbol* sig = _signature;
    int len = sig->utf8_length();
    if (_end >= len) {
      _end = len + 1;
      return;
    }

    _begin = _end;
    int t = sig->byte_at(_begin);
    switch (t) {
      case 'B': _type = T_BYTE;    break;
      case 'C': _type = T_CHAR;    break;
      case 'D': _type = T_DOUBLE;  break;
      case 'F': _type = T_FLOAT;   break;
      case 'I': _type = T_INT;     break;
      case 'J': _type = T_LONG;    break;
      case 'S': _type = T_SHORT;   break;
      case 'Z': _type = T_BOOLEAN; break;
      case 'V': _type = T_VOID;    break;
      default : next_non_primitive(t);
                return;
    }
    _end++;
  }

  SignatureStream(Symbol* signature, bool is_method = true);
  ~SignatureStream();

  bool is_object() const;                        // True if this argument is an object
  bool is_array() const;                         // True if this argument is an array
  BasicType type() const                         { return _type; }
  Symbol* as_symbol(TRAPS);
  enum FailureMode { ReturnNull, CNFException, NCDFError };
  Klass* as_klass(Handle class_loader, Handle protection_domain, FailureMode failure_mode, TRAPS);
  oop as_java_mirror(Handle class_loader, Handle protection_domain, FailureMode failure_mode, TRAPS);
  const jbyte* raw_bytes()  { return _signature->bytes() + _begin; }
  int          raw_length() { return _end - _begin; }

  // return same as_symbol except allocation of new symbols is avoided.
  Symbol* as_symbol_or_null();
};

class SignatureVerifier : public StackObj {
  public:
    // Returns true if the symbol is valid method or type signature
    static bool is_valid_signature(Symbol* sig);

    static bool is_valid_method_signature(Symbol* sig);
    static bool is_valid_type_signature(Symbol* sig);
  private:

    static ssize_t is_valid_type(const char*, ssize_t);
    static bool invalid_name_char(char);
};

#endif // SHARE_VM_RUNTIME_SIGNATURE_HPP
