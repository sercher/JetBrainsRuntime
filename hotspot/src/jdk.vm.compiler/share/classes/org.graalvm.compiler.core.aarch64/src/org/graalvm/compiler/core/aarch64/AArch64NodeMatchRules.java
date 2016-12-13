/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.compiler.core.aarch64;

import org.graalvm.compiler.core.gen.NodeMatchRules;
import org.graalvm.compiler.lir.LIRFrameState;
import org.graalvm.compiler.lir.gen.LIRGeneratorTool;
import org.graalvm.compiler.nodes.DeoptimizingNode;
import org.graalvm.compiler.nodes.memory.Access;

import jdk.vm.ci.aarch64.AArch64Kind;

public class AArch64NodeMatchRules extends NodeMatchRules {

    public AArch64NodeMatchRules(LIRGeneratorTool gen) {
        super(gen);
    }

    protected LIRFrameState getState(Access access) {
        if (access instanceof DeoptimizingNode) {
            return state((DeoptimizingNode) access);
        }
        return null;
    }

    protected AArch64Kind getMemoryKind(Access access) {
        return (AArch64Kind) gen.getLIRKind(access.asNode().stamp()).getPlatformKind();
    }

    @Override
    public AArch64LIRGenerator getLIRGeneratorTool() {
        return (AArch64LIRGenerator) gen;
    }

    protected AArch64ArithmeticLIRGenerator getArithmeticLIRGenerator() {
        return (AArch64ArithmeticLIRGenerator) getLIRGeneratorTool().getArithmetic();
    }
}
