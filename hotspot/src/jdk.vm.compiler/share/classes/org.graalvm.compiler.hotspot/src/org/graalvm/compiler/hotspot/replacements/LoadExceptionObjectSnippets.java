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
package org.graalvm.compiler.hotspot.replacements;

import static org.graalvm.compiler.hotspot.meta.HotSpotForeignCallsProviderImpl.LOAD_AND_CLEAR_EXCEPTION;
import static org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.EXCEPTION_OOP_LOCATION;
import static org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.EXCEPTION_PC_LOCATION;
import static org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.readExceptionOop;
import static org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.registerAsWord;
import static org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.writeExceptionOop;
import static org.graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.writeExceptionPc;
import static org.graalvm.compiler.nodes.PiNode.piCast;
import static org.graalvm.compiler.replacements.SnippetTemplate.DEFAULT_REPLACER;

import org.graalvm.compiler.api.replacements.Snippet;
import org.graalvm.compiler.api.replacements.Snippet.ConstantParameter;
import org.graalvm.compiler.core.common.type.StampFactory;
import org.graalvm.compiler.hotspot.meta.HotSpotProviders;
import org.graalvm.compiler.hotspot.meta.HotSpotRegistersProvider;
import org.graalvm.compiler.nodes.StructuredGraph;
import org.graalvm.compiler.nodes.extended.ForeignCallNode;
import org.graalvm.compiler.nodes.java.LoadExceptionObjectNode;
import org.graalvm.compiler.nodes.spi.LoweringTool;
import org.graalvm.compiler.replacements.SnippetTemplate.AbstractTemplates;
import org.graalvm.compiler.replacements.SnippetTemplate.Arguments;
import org.graalvm.compiler.replacements.SnippetTemplate.SnippetInfo;
import org.graalvm.compiler.replacements.Snippets;
import org.graalvm.compiler.replacements.nodes.ReadRegisterNode;
import org.graalvm.compiler.word.Word;

import jdk.vm.ci.code.BytecodeFrame;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.TargetDescription;

/**
 * Snippet for loading the exception object at the start of an exception dispatcher.
 * <p>
 * The frame state upon entry to an exception handler is such that it is a
 * {@link BytecodeFrame#rethrowException rethrow exception} state and the stack contains exactly the
 * exception object (per the JVM spec) to rethrow. This means that the code generated for this node
 * must not cause a deoptimization as the runtime/interpreter would not have a valid location to
 * find the exception object to be rethrown.
 */
public class LoadExceptionObjectSnippets implements Snippets {

    /**
     * Alternative way to implement exception object loading.
     */
    private static final boolean USE_C_RUNTIME = HotspotSnippetsOptions.LoadExceptionObjectInVM.getValue();

    @Snippet
    public static Object loadException(@ConstantParameter Register threadRegister) {
        Word thread = registerAsWord(threadRegister);
        Object exception = readExceptionOop(thread);
        writeExceptionOop(thread, null);
        writeExceptionPc(thread, Word.zero());
        return piCast(exception, StampFactory.forNodeIntrinsic());
    }

    public static class Templates extends AbstractTemplates {

        private final SnippetInfo loadException = snippet(LoadExceptionObjectSnippets.class, "loadException", EXCEPTION_OOP_LOCATION, EXCEPTION_PC_LOCATION);

        public Templates(HotSpotProviders providers, TargetDescription target) {
            super(providers, providers.getSnippetReflection(), target);
        }

        public void lower(LoadExceptionObjectNode loadExceptionObject, HotSpotRegistersProvider registers, LoweringTool tool) {
            if (USE_C_RUNTIME) {
                StructuredGraph graph = loadExceptionObject.graph();
                ReadRegisterNode thread = graph.add(new ReadRegisterNode(registers.getThreadRegister(), true, false));
                graph.addBeforeFixed(loadExceptionObject, thread);
                ForeignCallNode loadExceptionC = graph.add(new ForeignCallNode(providers.getForeignCalls(), LOAD_AND_CLEAR_EXCEPTION, thread));
                loadExceptionC.setStateAfter(loadExceptionObject.stateAfter());
                graph.replaceFixedWithFixed(loadExceptionObject, loadExceptionC);
            } else {
                Arguments args = new Arguments(loadException, loadExceptionObject.graph().getGuardsStage(), tool.getLoweringStage());
                args.addConst("threadRegister", registers.getThreadRegister());
                template(args).instantiate(providers.getMetaAccess(), loadExceptionObject, DEFAULT_REPLACER, args);
            }
        }
    }
}
