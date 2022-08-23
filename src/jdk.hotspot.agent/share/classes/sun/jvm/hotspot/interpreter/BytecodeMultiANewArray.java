/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.interpreter;

import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.utilities.*;

public class BytecodeMultiANewArray extends BytecodeWithKlass {
  BytecodeMultiANewArray(Method method, int bci)  {
    super(method, bci);
  }

  public Klass getKlass() {
    return super.getKlass();
  }

  public int getDimension() {
    return 0xFF & javaByteAt(2);
  }

  public void verify() {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(isValid(), "check multianewarray");
    }
  }

  public boolean isValid() {
    return javaCode() == Bytecodes._multianewarray;
  }

  public static BytecodeMultiANewArray at(Method method, int bci) {
    BytecodeMultiANewArray b = new BytecodeMultiANewArray(method, bci);
    if (Assert.ASSERTS_ENABLED) {
      b.verify();
    }
    return b;
  }

  /** Like at, but returns null if the BCI is not at multianewarray  */
  public static BytecodeMultiANewArray atCheck(Method method, int bci) {
    BytecodeMultiANewArray b = new BytecodeMultiANewArray(method, bci);
    return (b.isValid() ? b : null);
  }

  public static BytecodeMultiANewArray at(BytecodeStream bcs) {
    return new BytecodeMultiANewArray(bcs.method(), bcs.bci());
  }

  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append(super.toString());
    buf.append(spaces);
    buf.append(getDimension());
    return buf.toString();
  }
}
