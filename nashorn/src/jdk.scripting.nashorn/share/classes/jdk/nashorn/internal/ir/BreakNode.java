/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package jdk.nashorn.internal.ir;

import jdk.nashorn.internal.codegen.Label;
import jdk.nashorn.internal.ir.annotations.Immutable;
import jdk.nashorn.internal.ir.visitor.NodeVisitor;

/**
 * IR representation for {@code break} statements.
 */
@Immutable
public final class BreakNode extends JumpStatement {
    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     *
     * @param lineNumber line number
     * @param token      token
     * @param finish     finish
     * @param labelName  label name for break or null if none
     */
    public BreakNode(final int lineNumber, final long token, final int finish, final String labelName) {
        super(lineNumber, token, finish, labelName);
    }

    private BreakNode(final BreakNode breakNode, final LocalVariableConversion conversion) {
        super(breakNode, conversion);
    }

    @Override
    public Node accept(final NodeVisitor<? extends LexicalContext> visitor) {
        if (visitor.enterBreakNode(this)) {
            return visitor.leaveBreakNode(this);
        }

        return this;
    }

    @Override
    JumpStatement createNewJumpStatement(final LocalVariableConversion conversion) {
        return new BreakNode(this, conversion);
    }

    @Override
    String getStatementName() {
        return "break";
    }

    @Override
    public BreakableNode getTarget(final LexicalContext lc) {
        return lc.getBreakable(getLabelName());
    }

    @Override
    public Label getTargetLabel(final BreakableNode target) {
        return target.getBreakLabel();
    }
}
