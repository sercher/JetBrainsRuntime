/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.source.tree;

import java.util.List;
import javax.lang.model.element.Name;

/**
 * A tree node for a type parameter.
 *
 * For example:
 * <pre>
 *   <em>name</em>
 *
 *   <em>name</em> extends <em>bounds</em>
 *
 *   <em>annotations</em> <em>name</em>
 * </pre>
 *
 * @jls section 4.4
 *
 * @author Peter von der Ah&eacute;
 * @author Jonathan Gibbons
 * @since 1.6
 */
@jdk.Exported
public interface TypeParameterTree extends Tree {
    /**
     * Returns the name of the type parameter.
     * @return the name
     */
    Name getName();

    /**
     * Returns the bounds of the type parameter.
     * @return the bounds
     */
    List<? extends Tree> getBounds();

    /**
     * Returns annotations on the type parameter declaration.
     *
     * Annotations need Target meta-annotations of
     * {@link java.lang.annotation.ElementType#TYPE_PARAMETER} or
     * {@link java.lang.annotation.ElementType#TYPE_USE}
     * to appear in this position.
     *
     * @return annotations on the type parameter declaration
     * @since 1.8
     */
    List<? extends AnnotationTree> getAnnotations();
}
