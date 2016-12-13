/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.compiler.hotspot.sparc;

import org.graalvm.compiler.lir.LIRInstructionClass;
import org.graalvm.compiler.lir.asm.CompilationResultBuilder;
import org.graalvm.compiler.lir.sparc.SPARCBlockEndOp;

/**
 * Superclass for operations that leave a method's frame.
 */
abstract class SPARCHotSpotEpilogueOp extends SPARCBlockEndOp {
    public static final LIRInstructionClass<SPARCHotSpotEpilogueOp> TYPE = LIRInstructionClass.create(SPARCHotSpotEpilogueOp.class);

    protected SPARCHotSpotEpilogueOp(LIRInstructionClass<? extends SPARCHotSpotEpilogueOp> c, SizeEstimate size) {
        super(c, size);
    }

    protected void leaveFrame(CompilationResultBuilder crb) {
        crb.frameContext.leave(crb);
    }
}
