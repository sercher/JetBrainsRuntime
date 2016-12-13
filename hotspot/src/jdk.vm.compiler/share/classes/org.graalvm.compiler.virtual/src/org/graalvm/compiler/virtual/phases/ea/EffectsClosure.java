/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.compiler.virtual.phases.ea;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.graalvm.compiler.core.common.CollectionsFactory;
import org.graalvm.compiler.core.common.LocationIdentity;
import org.graalvm.compiler.core.common.cfg.BlockMap;
import org.graalvm.compiler.core.common.cfg.Loop;
import org.graalvm.compiler.core.common.type.Stamp;
import org.graalvm.compiler.debug.Debug;
import org.graalvm.compiler.debug.GraalError;
import org.graalvm.compiler.debug.Indent;
import org.graalvm.compiler.graph.Node;
import org.graalvm.compiler.graph.NodeBitMap;
import org.graalvm.compiler.graph.NodeMap;
import org.graalvm.compiler.graph.iterators.NodeIterable;
import org.graalvm.compiler.nodes.AbstractMergeNode;
import org.graalvm.compiler.nodes.FixedWithNextNode;
import org.graalvm.compiler.nodes.IfNode;
import org.graalvm.compiler.nodes.LogicConstantNode;
import org.graalvm.compiler.nodes.LogicNode;
import org.graalvm.compiler.nodes.LoopBeginNode;
import org.graalvm.compiler.nodes.LoopExitNode;
import org.graalvm.compiler.nodes.PhiNode;
import org.graalvm.compiler.nodes.ProxyNode;
import org.graalvm.compiler.nodes.StructuredGraph;
import org.graalvm.compiler.nodes.StructuredGraph.ScheduleResult;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.nodes.ValuePhiNode;
import org.graalvm.compiler.nodes.cfg.Block;
import org.graalvm.compiler.nodes.cfg.ControlFlowGraph;
import org.graalvm.compiler.nodes.extended.BoxNode;
import org.graalvm.compiler.nodes.util.GraphUtil;
import org.graalvm.compiler.nodes.virtual.AllocatedObjectNode;
import org.graalvm.compiler.nodes.virtual.CommitAllocationNode;
import org.graalvm.compiler.nodes.virtual.VirtualObjectNode;
import org.graalvm.compiler.phases.graph.ReentrantBlockIterator;
import org.graalvm.compiler.phases.graph.ReentrantBlockIterator.BlockIteratorClosure;
import org.graalvm.compiler.phases.graph.ReentrantBlockIterator.LoopInfo;

public abstract class EffectsClosure<BlockT extends EffectsBlockState<BlockT>> extends EffectsPhase.Closure<BlockT> {

    protected final ControlFlowGraph cfg;
    protected final ScheduleResult schedule;

    protected final NodeMap<ValueNode> aliases;
    protected final BlockMap<GraphEffectList> blockEffects;
    private final Map<Loop<Block>, GraphEffectList> loopMergeEffects = CollectionsFactory.newIdentityMap();
    // Intended to be used by read-eliminating phases based on the effects phase.
    protected final Map<Loop<Block>, LoopKillCache> loopLocationKillCache = CollectionsFactory.newIdentityMap();
    private final Map<LoopBeginNode, BlockT> loopEntryStates = Node.newIdentityMap();
    private final NodeBitMap hasScalarReplacedInputs;

    protected boolean changed;

    public EffectsClosure(ScheduleResult schedule, ControlFlowGraph cfg) {
        this.schedule = schedule;
        this.cfg = cfg;
        this.aliases = cfg.graph.createNodeMap();
        this.hasScalarReplacedInputs = cfg.graph.createNodeBitMap();
        this.blockEffects = new BlockMap<>(cfg);
        for (Block block : cfg.getBlocks()) {
            blockEffects.put(block, new GraphEffectList());
        }
    }

    @Override
    public boolean hasChanged() {
        return changed;
    }

    @Override
    public void applyEffects() {
        final StructuredGraph graph = cfg.graph;
        final ArrayList<Node> obsoleteNodes = new ArrayList<>(0);
        final ArrayList<GraphEffectList> effectList = new ArrayList<>();
        BlockIteratorClosure<Void> closure = new BlockIteratorClosure<Void>() {

            @Override
            protected Void getInitialState() {
                return null;
            }

            private void apply(GraphEffectList effects) {
                if (effects != null && !effects.isEmpty()) {
                    effectList.add(effects);
                }
            }

            @Override
            protected Void processBlock(Block block, Void currentState) {
                apply(blockEffects.get(block));
                return currentState;
            }

            @Override
            protected Void merge(Block merge, List<Void> states) {
                return null;
            }

            @Override
            protected Void cloneState(Void oldState) {
                return oldState;
            }

            @Override
            protected List<Void> processLoop(Loop<Block> loop, Void initialState) {
                LoopInfo<Void> info = ReentrantBlockIterator.processLoop(this, loop, initialState);
                apply(loopMergeEffects.get(loop));
                return info.exitStates;
            }
        };
        ReentrantBlockIterator.apply(closure, cfg.getStartBlock());
        for (GraphEffectList effects : effectList) {
            Debug.log(" ==== effects");
            effects.apply(graph, obsoleteNodes, false);
        }
        for (GraphEffectList effects : effectList) {
            Debug.log(" ==== cfg kill effects");
            effects.apply(graph, obsoleteNodes, true);
        }
        Debug.dump(Debug.VERBOSE_LOG_LEVEL, graph, "After applying effects");
        assert VirtualUtil.assertNonReachable(graph, obsoleteNodes);
        for (Node node : obsoleteNodes) {
            if (node.isAlive()) {
                node.replaceAtUsages(null);
                GraphUtil.killWithUnusedFloatingInputs(node);
            }
        }
    }

    @Override
    protected BlockT processBlock(Block block, BlockT state) {
        if (!state.isDead()) {
            GraphEffectList effects = blockEffects.get(block);

            if (block.getBeginNode().predecessor() instanceof IfNode) {
                IfNode ifNode = (IfNode) block.getBeginNode().predecessor();
                LogicNode condition = ifNode.condition();
                Node alias = getScalarAlias(condition);
                if (alias instanceof LogicConstantNode) {
                    LogicConstantNode constant = (LogicConstantNode) alias;
                    boolean deadBranch = constant.getValue() != (block.getBeginNode() == ifNode.trueSuccessor());

                    if (deadBranch) {
                        state.markAsDead();
                        effects.killIfBranch(ifNode, constant.getValue());
                        return state;
                    }
                }
            }

            VirtualUtil.trace("\nBlock: %s, preds: %s, succ: %s (", block, block.getPredecessors(), block.getSuccessors());

            FixedWithNextNode lastFixedNode = block.getBeginNode().predecessor() instanceof FixedWithNextNode ? (FixedWithNextNode) block.getBeginNode().predecessor() : null;
            Iterable<? extends Node> nodes = schedule != null ? schedule.getBlockToNodesMap().get(block) : block.getNodes();
            for (Node node : nodes) {
                aliases.set(node, null);
                if (node instanceof LoopExitNode) {
                    LoopExitNode loopExit = (LoopExitNode) node;
                    for (ProxyNode proxy : loopExit.proxies()) {
                        aliases.set(proxy, null);
                        changed |= processNode(proxy, state, effects, lastFixedNode) && isSignificantNode(node);
                    }
                    processLoopExit(loopExit, loopEntryStates.get(loopExit.loopBegin()), state, blockEffects.get(block));
                }
                changed |= processNode(node, state, effects, lastFixedNode) && isSignificantNode(node);
                if (node instanceof FixedWithNextNode) {
                    lastFixedNode = (FixedWithNextNode) node;
                }
                if (state.isDead()) {
                    break;
                }
            }
            VirtualUtil.trace(")\n    end state: %s\n", state);
        }
        return state;
    }

    private static boolean isSignificantNode(Node node) {
        return !(node instanceof CommitAllocationNode || node instanceof AllocatedObjectNode || node instanceof BoxNode);
    }

    /**
     * Collects the effects of virtualizing the given node.
     *
     * @return {@code true} if the effects include removing the node, {@code false} otherwise.
     */
    protected abstract boolean processNode(Node node, BlockT state, GraphEffectList effects, FixedWithNextNode lastFixedNode);

    @Override
    protected BlockT merge(Block merge, List<BlockT> states) {
        assert blockEffects.get(merge).isEmpty();
        MergeProcessor processor = createMergeProcessor(merge);
        doMergeWithoutDead(processor, states);
        processor.commitEnds(states);
        blockEffects.get(merge).addAll(processor.mergeEffects);
        blockEffects.get(merge).addAll(processor.afterMergeEffects);
        return processor.newState;
    }

    @Override
    @SuppressWarnings("try")
    protected final List<BlockT> processLoop(Loop<Block> loop, BlockT initialState) {
        if (initialState.isDead()) {
            ArrayList<BlockT> states = new ArrayList<>();
            for (int i = 0; i < loop.getExits().size(); i++) {
                states.add(initialState);
            }
            return states;
        }
        /*
         * Special case nested loops: To avoid an exponential runtime for nested loops we try to
         * only process them as little times as possible.
         *
         * In the first iteration of an outer most loop we go into the inner most loop(s). We run
         * the first iteration of the inner most loop and then, if necessary, a second iteration.
         *
         * We return from the recursion and finish the first iteration of the outermost loop. If we
         * have to do a second iteration in the outer most loop we go again into the inner most
         * loop(s) but this time we already know all states that are killed by the loop so inside
         * the loop we will only have those changes that propagate from the first iteration of the
         * outer most loop into the current loop. We strip the initial loop state for the inner most
         * loops and do the first iteration with the (possible) changes from outer loops. If there
         * are no changes we only have to do 1 iteration and are done.
         *
         */
        BlockT initialStateRemovedKilledLocations = stripKilledLoopLocations(loop, cloneState(initialState));
        BlockT loopEntryState = initialStateRemovedKilledLocations;
        BlockT lastMergedState = cloneState(initialStateRemovedKilledLocations);
        processInitialLoopState(loop, lastMergedState);
        MergeProcessor mergeProcessor = createMergeProcessor(loop.getHeader());
        for (int iteration = 0; iteration < 10; iteration++) {
            try (Indent i = Debug.logAndIndent("================== Process Loop Effects Closure: block:%s begin node:%s", loop.getHeader(), loop.getHeader().getBeginNode())) {
                LoopInfo<BlockT> info = ReentrantBlockIterator.processLoop(this, loop, cloneState(lastMergedState));

                List<BlockT> states = new ArrayList<>();
                states.add(initialStateRemovedKilledLocations);
                states.addAll(info.endStates);
                doMergeWithoutDead(mergeProcessor, states);

                Debug.log("MergeProcessor New State: %s", mergeProcessor.newState);
                Debug.log("===== vs.");
                Debug.log("Last Merged State: %s", lastMergedState);

                if (mergeProcessor.newState.equivalentTo(lastMergedState)) {
                    mergeProcessor.commitEnds(states);

                    blockEffects.get(loop.getHeader()).insertAll(mergeProcessor.mergeEffects, 0);
                    loopMergeEffects.put(loop, mergeProcessor.afterMergeEffects);

                    assert info.exitStates.size() == loop.getExits().size();
                    loopEntryStates.put((LoopBeginNode) loop.getHeader().getBeginNode(), loopEntryState);
                    assert assertExitStatesNonEmpty(loop, info);

                    processKilledLoopLocations(loop, initialStateRemovedKilledLocations, mergeProcessor.newState);
                    return info.exitStates;
                } else {
                    lastMergedState = mergeProcessor.newState;
                    for (Block block : loop.getBlocks()) {
                        blockEffects.get(block).clear();
                    }
                }
            }
        }
        throw new GraalError("too many iterations at %s", loop);
    }

    @SuppressWarnings("unused")
    protected BlockT stripKilledLoopLocations(Loop<Block> loop, BlockT initialState) {
        return initialState;
    }

    @SuppressWarnings("unused")
    protected void processKilledLoopLocations(Loop<Block> loop, BlockT initialState, BlockT mergedStates) {
        // nothing to do
    }

    @SuppressWarnings("unused")
    protected void processInitialLoopState(Loop<Block> loop, BlockT initialState) {
        // nothing to do
    }

    private void doMergeWithoutDead(MergeProcessor mergeProcessor, List<BlockT> states) {
        int alive = 0;
        for (BlockT state : states) {
            if (!state.isDead()) {
                alive++;
            }
        }
        if (alive == 0) {
            mergeProcessor.setNewState(states.get(0));
        } else if (alive == states.size()) {
            int[] stateIndexes = new int[states.size()];
            for (int i = 0; i < stateIndexes.length; i++) {
                stateIndexes[i] = i;
            }
            mergeProcessor.setStateIndexes(stateIndexes);
            mergeProcessor.merge(states);
        } else {
            ArrayList<BlockT> aliveStates = new ArrayList<>(alive);
            int[] stateIndexes = new int[alive];
            for (int i = 0; i < states.size(); i++) {
                if (!states.get(i).isDead()) {
                    stateIndexes[aliveStates.size()] = i;
                    aliveStates.add(states.get(i));
                }
            }
            mergeProcessor.setStateIndexes(stateIndexes);
            mergeProcessor.merge(aliveStates);
        }
    }

    private boolean assertExitStatesNonEmpty(Loop<Block> loop, LoopInfo<BlockT> info) {
        for (int i = 0; i < loop.getExits().size(); i++) {
            assert info.exitStates.get(i) != null : "no loop exit state at " + loop.getExits().get(i) + " / " + loop.getHeader();
        }
        return true;
    }

    protected abstract void processLoopExit(LoopExitNode exitNode, BlockT initialState, BlockT exitState, GraphEffectList effects);

    protected abstract MergeProcessor createMergeProcessor(Block merge);

    protected class MergeProcessor {

        private final Block mergeBlock;
        private final AbstractMergeNode merge;

        protected final GraphEffectList mergeEffects;
        protected final GraphEffectList afterMergeEffects;

        private int[] stateIndexes;
        protected BlockT newState;

        public MergeProcessor(Block mergeBlock) {
            this.mergeBlock = mergeBlock;
            this.merge = (AbstractMergeNode) mergeBlock.getBeginNode();
            this.mergeEffects = new GraphEffectList();
            this.afterMergeEffects = new GraphEffectList();
        }

        /**
         * @param states the states that should be merged.
         */
        protected void merge(List<BlockT> states) {
            setNewState(getInitialState());
        }

        private void setNewState(BlockT state) {
            newState = state;
            mergeEffects.clear();
            afterMergeEffects.clear();
        }

        private void setStateIndexes(int[] stateIndexes) {
            this.stateIndexes = stateIndexes;
        }

        @SuppressWarnings("unused")
        protected void commitEnds(List<BlockT> states) {
        }

        protected final Block getPredecessor(int index) {
            return mergeBlock.getPredecessors()[stateIndexes[index]];
        }

        protected final NodeIterable<PhiNode> getPhis() {
            return merge.phis();
        }

        protected final ValueNode getPhiValueAt(PhiNode phi, int index) {
            return phi.valueAt(stateIndexes[index]);
        }

        protected final ValuePhiNode createValuePhi(Stamp stamp) {
            return new ValuePhiNode(stamp, merge, new ValueNode[mergeBlock.getPredecessorCount()]);
        }

        protected final void setPhiInput(PhiNode phi, int index, ValueNode value) {
            afterMergeEffects.initializePhiInput(phi, stateIndexes[index], value);
        }

        protected final int getStateIndex(int i) {
            return stateIndexes[i];
        }

        protected final StructuredGraph graph() {
            return merge.graph();
        }

        @Override
        public String toString() {
            return "MergeProcessor@" + merge;
        }
    }

    public void addScalarAlias(ValueNode node, ValueNode alias) {
        assert !(alias instanceof VirtualObjectNode);
        aliases.set(node, alias);
        for (Node usage : node.usages()) {
            if (!hasScalarReplacedInputs.isNew(usage)) {
                hasScalarReplacedInputs.mark(usage);
            }
        }
    }

    protected final boolean hasScalarReplacedInputs(Node node) {
        return hasScalarReplacedInputs.isMarked(node);
    }

    public ValueNode getScalarAlias(ValueNode node) {
        assert !(node instanceof VirtualObjectNode);
        if (node == null || !node.isAlive() || aliases.isNew(node)) {
            return node;
        }
        ValueNode result = aliases.get(node);
        return (result == null || result instanceof VirtualObjectNode) ? node : result;
    }

    protected static class LoopKillCache {
        private int visits;
        private LocationIdentity firstLocation;
        private Set<LocationIdentity> killedLocations;
        private boolean killsAll;

        protected LoopKillCache(int visits) {
            this.visits = visits;
        }

        protected void visited() {
            visits++;
        }

        protected int visits() {
            return visits;
        }

        protected void setKillsAll() {
            killsAll = true;
            firstLocation = null;
            killedLocations = null;
        }

        protected boolean containsLocation(LocationIdentity locationIdentity) {
            if (killsAll) {
                return true;
            }
            if (firstLocation == null) {
                return false;
            }
            if (!firstLocation.equals(locationIdentity)) {
                return killedLocations != null ? killedLocations.contains(locationIdentity) : false;
            }
            return true;
        }

        protected void rememberLoopKilledLocation(LocationIdentity locationIdentity) {
            if (killsAll) {
                return;
            }
            if (firstLocation == null || firstLocation.equals(locationIdentity)) {
                firstLocation = locationIdentity;
            } else {
                if (killedLocations == null) {
                    killedLocations = new HashSet<>();
                }
                killedLocations.add(locationIdentity);
            }
        }

        protected boolean loopKillsLocations() {
            if (killsAll) {
                return true;
            }
            return firstLocation != null;
        }
    }

}
