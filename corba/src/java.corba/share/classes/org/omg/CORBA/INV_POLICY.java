/*
 * Copyright (c) 1998, 2017, Oracle and/or its affiliates. All rights reserved.
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

package org.omg.CORBA;

/**
 * Standard exception  thrown
 * when an invocation cannot be made because of an incompatibility between
 * {@code Policy} overrides that apply to the particular invocation.
 * It contains a minor code, which gives more detailed information about
 * what caused the exception, and a completion status. It may also contain
 * a string describing the exception.
 *
 * <p>See also {@extLink jidlexception documentation on Java&nbsp;IDL exceptions}.
 * </p>
 */

public final class INV_POLICY extends SystemException {
    /**
     * Constructs a {@code INV_POLICY} exception with a default minor code
     * of 0, a completion state of CompletionStatus.COMPLETED_NO,
     * and a null description.
     */
    public INV_POLICY() {
        this("");
    }

    /**
     * Constructs a {@code INV_POLICY} exception with the
     * specified description message,
     * a minor code of 0, and a completion state of COMPLETED_NO.
     * @param s the String containing a detail message
     */
    public INV_POLICY(String s) {
        this(s, 0, CompletionStatus.COMPLETED_NO);
    }

    /**
     * Constructs a {@code INV_POLICY} exception with the specified
     * minor code and completion status.
     * @param minor the minor code
     * @param completed the completion status
     */
    public INV_POLICY(int minor, CompletionStatus completed) {
        this("", minor, completed);
    }

    /**
     * Constructs a {@code INV_POLICY} exception with the
     * specified description message, minor code, and completion status.
     * @param s the String containing a description message
     * @param minor the minor code
     * @param completed the completion status
     */
    public INV_POLICY(String s, int minor, CompletionStatus completed) {
        super(s, minor, completed);
    }
}
