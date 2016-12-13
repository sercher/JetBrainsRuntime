/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.compiler.lir.constopt;

import org.graalvm.compiler.core.common.cfg.AbstractBlockBase;
import org.graalvm.compiler.lir.LIRInstruction;
import org.graalvm.compiler.lir.ValueProcedure;

import jdk.vm.ci.meta.Value;

/**
 * Represents a usage of a constant.
 */
class UseEntry {

    private final AbstractBlockBase<?> block;
    private final LIRInstruction instruction;
    private final Value value;

    UseEntry(AbstractBlockBase<?> block, LIRInstruction instruction, Value value) {
        this.block = block;
        this.instruction = instruction;
        this.value = value;
    }

    public LIRInstruction getInstruction() {
        return instruction;
    }

    public AbstractBlockBase<?> getBlock() {
        return block;
    }

    public void setValue(Value newValue) {
        replaceValue(instruction, value, newValue);
    }

    private static void replaceValue(LIRInstruction op, Value oldValue, Value newValue) {
        ValueProcedure proc = (value, mode, flags) -> value.identityEquals(oldValue) ? newValue : value;
        op.forEachAlive(proc);
        op.forEachInput(proc);
        op.forEachOutput(proc);
        op.forEachTemp(proc);
        op.forEachState(proc);
    }

    public Value getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Use[" + getValue() + ":" + instruction + ":" + block + "]";
    }

}
