/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

package compiler.lib.ir_framework.driver.irmatching.irmethod;

import compiler.lib.ir_framework.driver.irmatching.irrule.IRRuleMatchResult;

/**
 * This class represents an IR matching result where the compilation output was empty.
 *
 * @see IRRuleMatchResult
 * @see IRMethod
 */
public class MissingCompilationResult extends IRMethodMatchResult {
    private final int failedIRRules;
    private final MissingCompilationMessageBuilder failureMessageBuilder;

    MissingCompilationResult(IRMethod irMethod, int failedIRRules) {
        super(irMethod);
        this.failedIRRules = failedIRRules;
        this.failureMessageBuilder = new MissingCompilationMessageBuilder(irMethod);
    }

    @Override
    public boolean fail() {
        return true;
    }

    @Override
    public String getMatchedCompilationOutput() {
        return "<empty>";
    }

    @Override
    public String buildFailureMessage() {
        return failureMessageBuilder.build();
    }

    private String getMethodLine() {
        return " Method \"" + irMethod.getMethod() + "\":" + System.lineSeparator();
    }

    @Override
    public int getFailedIRRuleCount() {
        return failedIRRules;
    }

}
