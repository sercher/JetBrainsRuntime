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

package jdk.nashorn.internal.codegen;

import java.util.List;
import java.util.Map;
import jdk.nashorn.internal.codegen.types.Type;
import jdk.nashorn.internal.ir.AccessNode;
import jdk.nashorn.internal.ir.BinaryNode;
import jdk.nashorn.internal.ir.Block;
import jdk.nashorn.internal.ir.BreakNode;
import jdk.nashorn.internal.ir.CallNode;
import jdk.nashorn.internal.ir.CatchNode;
import jdk.nashorn.internal.ir.ContinueNode;
import jdk.nashorn.internal.ir.DoWhileNode;
import jdk.nashorn.internal.ir.ExecuteNode;
import jdk.nashorn.internal.ir.ForNode;
import jdk.nashorn.internal.ir.FunctionNode;
import jdk.nashorn.internal.ir.IdentNode;
import jdk.nashorn.internal.ir.IfNode;
import jdk.nashorn.internal.ir.IndexNode;
import jdk.nashorn.internal.ir.LiteralNode;
import jdk.nashorn.internal.ir.LiteralNode.ArrayLiteralNode;
import jdk.nashorn.internal.ir.LiteralNode.ArrayLiteralNode.ArrayUnit;
import jdk.nashorn.internal.ir.Node;
import jdk.nashorn.internal.ir.PropertyNode;
import jdk.nashorn.internal.ir.ReferenceNode;
import jdk.nashorn.internal.ir.ReturnNode;
import jdk.nashorn.internal.ir.RuntimeNode;
import jdk.nashorn.internal.ir.SplitNode;
import jdk.nashorn.internal.ir.SwitchNode;
import jdk.nashorn.internal.ir.ThrowNode;
import jdk.nashorn.internal.ir.TryNode;
import jdk.nashorn.internal.ir.UnaryNode;
import jdk.nashorn.internal.ir.VarNode;
import jdk.nashorn.internal.ir.WhileNode;
import jdk.nashorn.internal.ir.WithNode;
import jdk.nashorn.internal.ir.visitor.NodeOperatorVisitor;


/**
 * Computes the "byte code" weight of an AST segment. This is used
 * for Splitting too large class files
 */
final class WeighNodes extends NodeOperatorVisitor {
    /*
     * Weight constants.
     */
            static final long FUNCTION_WEIGHT  = 40;
    private static final long ACCESS_WEIGHT    = 4;
    private static final long ADD_WEIGHT       = 10;
    private static final long BREAK_WEIGHT     = 1;
    private static final long CALL_WEIGHT      = 10;
    private static final long CATCH_WEIGHT     = 10;
    private static final long CONTINUE_WEIGHT  = 1;
    private static final long IF_WEIGHT        = 2;
    private static final long LITERAL_WEIGHT   = 10;
    private static final long LOOP_WEIGHT      = 4;
    private static final long NEW_WEIGHT       = 6;
    private static final long REFERENCE_WEIGHT = 20;
    private static final long RETURN_WEIGHT    = 2;
    private static final long SPLIT_WEIGHT     = 40;
    private static final long SWITCH_WEIGHT    = 8;
    private static final long THROW_WEIGHT     = 2;
    private static final long VAR_WEIGHT       = 40;
    private static final long WITH_WEIGHT      = 8;

    /** Accumulated weight. */
    private long weight;

    /** Optional cache for weight of block nodes. */
    private final Map<Node, Long> weightCache;

    /*
     * Constructor
     *
     * @param weightCache cache of already calculated block weights
     */
    private WeighNodes(final Map<Node, Long> weightCache) {
        super(null, null);
        this.weightCache = weightCache;
    }

    static long weigh(final Node node) {
        final WeighNodes weighNodes = new WeighNodes(null);
        node.accept(weighNodes);
        return weighNodes.weight;
    }

    static long weigh(final Node node, final Map<Node, Long> weightCache) {
        final WeighNodes weighNodes = new WeighNodes(weightCache);
        node.accept(weighNodes);
        return weighNodes.weight;
    }

    @Override
    public Node leave(final AccessNode accessNode) {
        weight += ACCESS_WEIGHT;
        return accessNode;
    }

    @Override
    public Node enter(final Block block) {
        if (weightCache != null && weightCache.containsKey(block)) {
            weight += weightCache.get(block);
            return null;
        }

        return block;
    }

    @Override
    public Node leave(final BreakNode breakNode) {
        weight += BREAK_WEIGHT;
        return breakNode;
    }

    @Override
    public Node leave(final CallNode callNode) {
        weight += CALL_WEIGHT;
        return callNode;
    }

    @Override
    public Node leave(final CatchNode catchNode) {
        weight += CATCH_WEIGHT;
        return catchNode;
    }

    @Override
    public Node leave(final ContinueNode continueNode) {
        weight += CONTINUE_WEIGHT;
        return continueNode;
    }

    @Override
    public Node leave(final DoWhileNode doWhileNode) {
        weight += LOOP_WEIGHT;
        return doWhileNode;
    }

    @Override
    public Node leave(final ExecuteNode executeNode) {
        return executeNode;
    }

    @Override
    public Node leave(final ForNode forNode) {
        weight += LOOP_WEIGHT;
        return forNode;
    }

    @Override
    public Node enter(final FunctionNode functionNode) {
        final List<Node> statements = functionNode.getStatements();

        for (final Node statement : statements) {
            statement.accept(this);
        }

        return null;
    }

    @Override
    public Node leave(final IdentNode identNode) {
        weight += ACCESS_WEIGHT + identNode.getName().length() * 2;
        return identNode;
    }

    @Override
    public Node leave(final IfNode ifNode) {
        weight += IF_WEIGHT;
        return ifNode;
    }

    @Override
    public Node leave(final IndexNode indexNode) {
        weight += ACCESS_WEIGHT;
        return indexNode;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Node enter(final LiteralNode literalNode) {
        weight += LITERAL_WEIGHT;

        if (literalNode instanceof ArrayLiteralNode) {
            final ArrayLiteralNode arrayLiteralNode = (ArrayLiteralNode)literalNode;
            final Node[]           value            = arrayLiteralNode.getValue();
            final int[]            postsets         = arrayLiteralNode.getPostsets();
            final List<ArrayUnit>  units            = arrayLiteralNode.getUnits();

            if (units == null) {
                for (final int postset : postsets) {
                    final Node element = value[postset];

                    if (element != null) {
                        element.accept(this);
                    }
                }
            }

            return null;
        }

        return literalNode;
    }

    @Override
    public Node leave(final PropertyNode propertyNode) {
        weight += LITERAL_WEIGHT;
        return propertyNode;
    }

    @Override
    public Node leave(final ReferenceNode referenceNode) {
        weight += REFERENCE_WEIGHT;
        return referenceNode;
    }

    @Override
    public Node leave(final ReturnNode returnNode) {
        weight += RETURN_WEIGHT;
        return returnNode;
    }

    @Override
    public Node leave(final RuntimeNode runtimeNode) {
        weight += CALL_WEIGHT;
        return runtimeNode;
    }

    @Override
    public Node enter(final SplitNode splitNode) {
        weight += SPLIT_WEIGHT;
        return null;
    }

    @Override
    public Node leave(final SwitchNode switchNode) {
        weight += SWITCH_WEIGHT;
        return switchNode;
    }

    @Override
    public Node leave(final ThrowNode throwNode) {
        weight += THROW_WEIGHT;
        return throwNode;
    }

    @Override
    public Node leave(final TryNode tryNode) {
        weight += THROW_WEIGHT;
        return tryNode;
    }

    @Override
    public Node leave(final VarNode varNode) {
        weight += VAR_WEIGHT;
        return varNode;
    }

    @Override
    public Node leave(final WhileNode whileNode) {
        weight += LOOP_WEIGHT;
        return whileNode;
    }

    @Override
    public Node leave(final WithNode withNode) {
        weight += WITH_WEIGHT;
        return withNode;
    }

    @Override
    public Node leaveADD(final UnaryNode unaryNode) {
        return unaryNodeWeight(unaryNode);
    }

    @Override
    public Node leaveBIT_NOT(final UnaryNode unaryNode) {
        return unaryNodeWeight(unaryNode);
    }

    @Override
    public Node leaveCONVERT(final UnaryNode unaryNode) {
        return unaryNodeWeight(unaryNode);
    }

    @Override
    public Node leaveDECINC(final UnaryNode unaryNode) {
         return unaryNodeWeight(unaryNode);
    }

    @Override
    public Node leaveDELETE(final UnaryNode unaryNode) {
        return runtimeNodeWeight(unaryNode);
    }

    @Override
    public Node leaveDISCARD(final UnaryNode unaryNode) {
        return unaryNodeWeight(unaryNode);
    }

    @Override
    public Node leaveNEW(final UnaryNode unaryNode) {
        weight += NEW_WEIGHT;
        return unaryNode;
    }

    @Override
    public Node leaveNOT(final UnaryNode unaryNode) {
        return unaryNodeWeight(unaryNode);
    }

    @Override
    public Node leaveSUB(final UnaryNode unaryNode) {
        return unaryNodeWeight(unaryNode);
    }

    @Override
    public Node leaveTYPEOF(final UnaryNode unaryNode) {
        return runtimeNodeWeight(unaryNode);
    }

    @Override
    public Node leaveVOID(final UnaryNode unaryNode) {
        return unaryNodeWeight(unaryNode);
    }

    @Override
    public Node leaveADD(final BinaryNode binaryNode) {
        weight += ADD_WEIGHT;
        return binaryNode;
    }

    @Override
    public Node leaveAND(final BinaryNode binaryNode) {
        return binaryNodeWeight(binaryNode);
    }

    @Override
    public Node leaveASSIGN(final BinaryNode binaryNode) {
        return binaryNodeWeight(binaryNode);
    }

    @Override
    public Node leaveASSIGN_ADD(final BinaryNode binaryNode) {
        weight += ADD_WEIGHT;
        return binaryNode;
    }

    @Override
    public Node leaveASSIGN_BIT_AND(final BinaryNode binaryNode) {
        return binaryNodeWeight(binaryNode);
    }

    @Override
    public Node leaveASSIGN_BIT_OR(final BinaryNode binaryNode) {
        return binaryNodeWeight(binaryNode);
    }

    @Override
    public Node leaveASSIGN_BIT_XOR(final BinaryNode binaryNode) {
        return binaryNodeWeight(binaryNode);
    }

    @Override
    public Node leaveASSIGN_DIV(final BinaryNode binaryNode) {
        return binaryNodeWeight(binaryNode);
    }

    @Override
    public Node leaveASSIGN_MOD(final BinaryNode binaryNode) {
        return binaryNodeWeight(binaryNode);
    }

    @Override
    public Node leaveASSIGN_MUL(final BinaryNode binaryNode) {
        return binaryNodeWeight(binaryNode);
    }

    @Override
    public Node leaveASSIGN_SAR(final BinaryNode binaryNode) {
        return binaryNodeWeight(binaryNode);
    }

    @Override
    public Node leaveASSIGN_SHL(final BinaryNode binaryNode) {
        return binaryNodeWeight(binaryNode);
    }

    @Override
    public Node leaveASSIGN_SHR(final BinaryNode binaryNode) {
        return binaryNodeWeight(binaryNode);
    }

    @Override
    public Node leaveASSIGN_SUB(final BinaryNode binaryNode) {
        return binaryNodeWeight(binaryNode);
    }

    @Override
    public Node leaveBIND(final BinaryNode binaryNode) {
        return binaryNodeWeight(binaryNode);
    }

    @Override
    public Node leaveBIT_AND(final BinaryNode binaryNode) {
        return binaryNodeWeight(binaryNode);
    }

    @Override
    public Node leaveBIT_OR(final BinaryNode binaryNode) {
        return binaryNodeWeight(binaryNode);
    }

    @Override
    public Node leaveBIT_XOR(final BinaryNode binaryNode) {
        return binaryNodeWeight(binaryNode);
    }

    @Override
    public Node leaveCOMMALEFT(final BinaryNode binaryNode) {
        return binaryNodeWeight(binaryNode);
    }

    @Override
    public Node leaveCOMMARIGHT(final BinaryNode binaryNode) {
        return binaryNodeWeight(binaryNode);
    }

    @Override
    public Node leaveDIV(final BinaryNode binaryNode) {
        return binaryNodeWeight(binaryNode);
    }

    @Override
    public Node leaveEQ(final BinaryNode binaryNode) {
        return runtimeNodeWeight(binaryNode);
    }

    @Override
    public Node leaveEQ_STRICT(final BinaryNode binaryNode) {
        return runtimeNodeWeight(binaryNode);
    }

    @Override
    public Node leaveGE(final BinaryNode binaryNode) {
        return runtimeNodeWeight(binaryNode);
    }

    @Override
    public Node leaveGT(final BinaryNode binaryNode) {
        return runtimeNodeWeight(binaryNode);
    }

    @Override
    public Node leaveIN(final BinaryNode binaryNode) {
        weight += CALL_WEIGHT;
        return binaryNode;
    }

    @Override
    public Node leaveINSTANCEOF(final BinaryNode binaryNode) {
        weight += CALL_WEIGHT;
        return binaryNode;
    }

    @Override
    public Node leaveLE(final BinaryNode binaryNode) {
        return runtimeNodeWeight(binaryNode);
    }

    @Override
    public Node leaveLT(final BinaryNode binaryNode) {
        return runtimeNodeWeight(binaryNode);
    }

    @Override
    public Node leaveMOD(final BinaryNode binaryNode) {
        return binaryNodeWeight(binaryNode);
    }

    @Override
    public Node leaveMUL(final BinaryNode binaryNode) {
        return binaryNodeWeight(binaryNode);
    }

    @Override
    public Node leaveNE(final BinaryNode binaryNode) {
        return runtimeNodeWeight(binaryNode);
    }

    @Override
    public Node leaveNE_STRICT(final BinaryNode binaryNode) {
        return runtimeNodeWeight(binaryNode);
    }

    @Override
    public Node leaveOR(final BinaryNode binaryNode) {
        return binaryNodeWeight(binaryNode);
    }

    @Override
    public Node leaveSAR(final BinaryNode binaryNode) {
        return binaryNodeWeight(binaryNode);
    }

    @Override
    public Node leaveSHL(final BinaryNode binaryNode) {
        return binaryNodeWeight(binaryNode);
    }

    @Override
    public Node leaveSHR(final BinaryNode binaryNode) {
        return binaryNodeWeight(binaryNode);
    }

    @Override
    public Node leaveSUB(final BinaryNode binaryNode) {
        return binaryNodeWeight(binaryNode);
    }

    private Node unaryNodeWeight(final UnaryNode unaryNode) {
        weight += 1;
        return unaryNode;
    }

    private Node binaryNodeWeight(final BinaryNode binaryNode) {
        weight += 1;
        return binaryNode;
    }

    private Node runtimeNodeWeight(final UnaryNode unaryNode) {
        weight += CALL_WEIGHT;
        return unaryNode;
    }

    private Node runtimeNodeWeight(final BinaryNode binaryNode) {
        weight += Type.widest(binaryNode.lhs().getType(), binaryNode.rhs().getType()).isObject() ? CALL_WEIGHT : 1;
        return binaryNode;
    }
}
