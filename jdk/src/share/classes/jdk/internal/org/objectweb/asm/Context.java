/*
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package jdk.internal.org.objectweb.asm;

/**
 * Information about a class being parsed in a {@link ClassReader}.
 *
 * @author Eric Bruneton
 */
class Context {

    /**
     * Prototypes of the attributes that must be parsed for this class.
     */
    Attribute[] attrs;

    /**
     * The {@link ClassReader} option flags for the parsing of this class.
     */
    int flags;

    /**
     * The buffer used to read strings.
     */
    char[] buffer;

    /**
     * The start index of each bootstrap method.
     */
    int[] bootstrapMethods;

    /**
     * The access flags of the method currently being parsed.
     */
    int access;

    /**
     * The name of the method currently being parsed.
     */
    String name;

    /**
     * The descriptor of the method currently being parsed.
     */
    String desc;

    /**
     * The label objects, indexed by bytecode offset, of the method currently
     * being parsed (only bytecode offsets for which a label is needed have a
     * non null associated Label object).
     */
    Label[] labels;

    /**
     * The target of the type annotation currently being parsed.
     */
    int typeRef;

    /**
     * The path of the type annotation currently being parsed.
     */
    TypePath typePath;

    /**
     * The offset of the latest stack map frame that has been parsed.
     */
    int offset;

    /**
     * The labels corresponding to the start of the local variable ranges in the
     * local variable type annotation currently being parsed.
     */
    Label[] start;

    /**
     * The labels corresponding to the end of the local variable ranges in the
     * local variable type annotation currently being parsed.
     */
    Label[] end;

    /**
     * The local variable indices for each local variable range in the local
     * variable type annotation currently being parsed.
     */
    int[] index;

    /**
     * The encoding of the latest stack map frame that has been parsed.
     */
    int mode;

    /**
     * The number of locals in the latest stack map frame that has been parsed.
     */
    int localCount;

    /**
     * The number locals in the latest stack map frame that has been parsed,
     * minus the number of locals in the previous frame.
     */
    int localDiff;

    /**
     * The local values of the latest stack map frame that has been parsed.
     */
    Object[] local;

    /**
     * The stack size of the latest stack map frame that has been parsed.
     */
    int stackCount;

    /**
     * The stack values of the latest stack map frame that has been parsed.
     */
    Object[] stack;
}
