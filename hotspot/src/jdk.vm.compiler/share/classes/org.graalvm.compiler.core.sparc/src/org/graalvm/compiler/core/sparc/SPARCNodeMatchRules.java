/*
 * Copyright (c) 2009, 2015, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.compiler.core.sparc;

import static jdk.vm.ci.sparc.SPARCKind.BYTE;
import static jdk.vm.ci.sparc.SPARCKind.HWORD;
import static jdk.vm.ci.sparc.SPARCKind.WORD;
import static jdk.vm.ci.sparc.SPARCKind.XWORD;

import org.graalvm.compiler.core.common.LIRKind;
import org.graalvm.compiler.core.gen.NodeMatchRules;
import org.graalvm.compiler.core.match.ComplexMatchResult;
import org.graalvm.compiler.core.match.MatchRule;
import org.graalvm.compiler.debug.GraalError;
import org.graalvm.compiler.lir.LIRFrameState;
import org.graalvm.compiler.lir.gen.LIRGeneratorTool;
import org.graalvm.compiler.nodes.DeoptimizingNode;
import org.graalvm.compiler.nodes.calc.SignExtendNode;
import org.graalvm.compiler.nodes.calc.ZeroExtendNode;
import org.graalvm.compiler.nodes.memory.Access;

import jdk.vm.ci.sparc.SPARCKind;

/**
 * This class implements the SPARC specific portion of the LIR generator.
 */
public class SPARCNodeMatchRules extends NodeMatchRules {

    public SPARCNodeMatchRules(LIRGeneratorTool gen) {
        super(gen);
    }

    protected LIRFrameState getState(Access access) {
        if (access instanceof DeoptimizingNode) {
            return state((DeoptimizingNode) access);
        }
        return null;
    }

    private ComplexMatchResult emitSignExtendMemory(Access access, int fromBits, int toBits) {
        assert fromBits <= toBits && toBits <= 64;
        SPARCKind toKind = null;
        SPARCKind fromKind = null;
        if (fromBits == toBits) {
            return null;
        }
        toKind = toBits > 32 ? XWORD : WORD;
        switch (fromBits) {
            case 8:
                fromKind = BYTE;
                break;
            case 16:
                fromKind = HWORD;
                break;
            case 32:
                fromKind = WORD;
                break;
            default:
                throw GraalError.unimplemented("unsupported sign extension (" + fromBits + " bit -> " + toBits + " bit)");
        }
        SPARCKind localFromKind = fromKind;
        SPARCKind localToKind = toKind;
        return builder -> {
            return getLIRGeneratorTool().emitSignExtendLoad(LIRKind.value(localFromKind), LIRKind.value(localToKind), operand(access.getAddress()), getState(access));
        };
    }

    private ComplexMatchResult emitZeroExtendMemory(Access access, int fromBits, int toBits) {
        assert fromBits <= toBits && toBits <= 64;
        SPARCKind toKind = null;
        SPARCKind fromKind = null;
        if (fromBits == toBits) {
            return null;
        }
        toKind = toBits > 32 ? XWORD : WORD;
        switch (fromBits) {
            case 8:
                fromKind = BYTE;
                break;
            case 16:
                fromKind = HWORD;
                break;
            case 32:
                fromKind = WORD;
                break;
            default:
                throw GraalError.unimplemented("unsupported sign extension (" + fromBits + " bit -> " + toBits + " bit)");
        }
        SPARCKind localFromKind = fromKind;
        SPARCKind localToKind = toKind;
        return builder -> {
            // Loads are always zero extending load
            return getLIRGeneratorTool().emitZeroExtendLoad(LIRKind.value(localFromKind), LIRKind.value(localToKind), operand(access.getAddress()), getState(access));
        };
    }

    @MatchRule("(SignExtend Read=access)")
    @MatchRule("(SignExtend FloatingRead=access)")
    public ComplexMatchResult signExtend(SignExtendNode root, Access access) {
        return emitSignExtendMemory(access, root.getInputBits(), root.getResultBits());
    }

    @MatchRule("(ZeroExtend Read=access)")
    @MatchRule("(ZeroExtend FloatingRead=access)")
    public ComplexMatchResult zeroExtend(ZeroExtendNode root, Access access) {
        return emitZeroExtendMemory(access, root.getInputBits(), root.getResultBits());
    }

    @Override
    public SPARCLIRGenerator getLIRGeneratorTool() {
        return (SPARCLIRGenerator) super.getLIRGeneratorTool();
    }

    protected SPARCArithmeticLIRGenerator getArithmeticLIRGenerator() {
        return (SPARCArithmeticLIRGenerator) getLIRGeneratorTool().getArithmetic();
    }
}
