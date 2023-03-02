/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, Arm Limited. All rights reserved.
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
package jdk.internal.foreign.abi.aarch64.macos;

import jdk.internal.foreign.abi.aarch64.CallArranger;
import jdk.internal.foreign.abi.ABIDescriptor;

/**
 * AArch64 CallArranger specialized for macOS ABI.
 */
public class MacOsAArch64CallArranger extends CallArranger {

    @Override
    protected boolean varArgsOnStack() {
        // Variadic arguments are always passed on the stack
        return true;
    }

    @Override
    protected boolean requiresSubSlotStackPacking() {
        return true;
    }

    @Override
    protected ABIDescriptor abiDescriptor() {
        return C;
    }

    @Override
    protected boolean useIntRegsForVariadicFloatingPointArgs() {
        return false;
    }

    @Override
    protected boolean spillsVariadicStructsPartially() {
        return false;
    }

}
