/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.compiler.hotspot.nodes.aot;

import static org.graalvm.compiler.nodeinfo.NodeCycles.CYCLES_3;
import static org.graalvm.compiler.nodeinfo.NodeSize.SIZE_20;

import org.graalvm.compiler.graph.NodeClass;
import org.graalvm.compiler.hotspot.meta.HotSpotConstantLoadAction;
import org.graalvm.compiler.nodeinfo.NodeInfo;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.nodes.calc.FloatingNode;
import org.graalvm.compiler.nodes.spi.Lowerable;
import org.graalvm.compiler.nodes.spi.LoweringTool;

@NodeInfo(cycles = CYCLES_3, size = SIZE_20)
public class ResolveConstantNode extends FloatingNode implements Lowerable {
    public static final NodeClass<ResolveConstantNode> TYPE = NodeClass.create(ResolveConstantNode.class);

    @Input ValueNode value;
    protected HotSpotConstantLoadAction action;

    public ResolveConstantNode(ValueNode value, HotSpotConstantLoadAction action) {
        super(TYPE, value.stamp());
        this.value = value;
        this.action = action;
    }

    public ResolveConstantNode(ValueNode value) {
        super(TYPE, value.stamp());
        this.value = value;
        this.action = HotSpotConstantLoadAction.RESOLVE;
    }

    @Override
    public void lower(LoweringTool tool) {
        tool.getLowerer().lower(this, tool);
    }

    public ValueNode value() {
        return value;
    }

    public HotSpotConstantLoadAction action() {
        return action;
    }
}
