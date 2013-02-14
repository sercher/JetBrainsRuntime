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

package jdk.nashorn.internal.runtime;

import jdk.internal.dynalink.CallSiteDescriptor;
import jdk.internal.dynalink.beans.StaticClass;
import jdk.internal.dynalink.linker.GuardedInvocation;
import jdk.internal.dynalink.linker.LinkRequest;
import jdk.nashorn.internal.objects.NativeJava;
import jdk.nashorn.internal.objects.annotations.Attribute;
import jdk.nashorn.internal.objects.annotations.Function;

/**
 * An object that exposes Java packages and classes as its properties. Packages are exposed as objects that have further
 * sub-packages and classes as their properties. Normally, three instances of this class are exposed as built-in objects
 * in Nashorn: {@code "Packages"}, {@code "java"}, and {@code "javax"}. Typical usages are:
 * <pre>
 * var list = new java.util.ArrayList()
 * var sprocket = new Packages.com.acme.Sprocket()
 * </pre>
 * or you can store the type objects in a variable for later reuse:
 * <pre>
 * var ArrayList = java.util.ArrayList
 * var list = new ArrayList
 * </pre>
 * You can also use {@link NativeJava#type(Object, Object)} to access Java classes. These two statements are mostly
 * equivalent:
 * <pre>
 * var listType1 = java.util.ArrayList
 * var listType2 = Java.type("java.util.ArrayList")
 * </pre>
 * The difference is that {@code Java.type()} will throw an error if the class does not exist, while the first
 * expression will return an empty object, as it must treat all non-existent classes as potentially being further
 * subpackages. As such, {@code Java.type()} has the potential to catch typos earlier. A further difference is that
 * {@code Java.type()} doesn't recognize {@code .} (dot) as the separator between outer class name and inner class name,
 * it only recognizes the dollar sign. These are equivalent:
 * <pre>
 * var ftype1 = java.awt.geom.Arc2D$Float
 * var ftype2 = java.awt.geom.Arc2D.Float
 * var ftype3 = Java.asType("java.awt.geom.Arc2D$Float")
 * var ftype4 = Java.asType("java.awt.geom.Arc2D").Float
 * </pre>
 */
public final class NativeJavaPackage extends ScriptObject {
    /** Full name of package (includes path.) */
    private final String name;

    /**
     * Public constructor to be accessible from {@link jdk.nashorn.internal.objects.Global}
     * @param name  package name
     * @param proto proto
     */
    public NativeJavaPackage(final String name, final ScriptObject proto) {
        this.name = name;
        this.setProto(proto);
    }

    @Override
    public String getClassName() {
        return "JavaPackage";
    }

    @Override
    public boolean equals(final Object other) {
        if (other instanceof NativeJavaPackage) {
            return name.equals(((NativeJavaPackage)other).name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name == null ? 0 : name.hashCode();
    }

    /**
     * Get the full name of the package
     * @return the name
     */
    public String getName() {
        return name;
    }

    @Override
    public String safeToString() {
        return toString();
    }

    @Override
    public String toString() {
        return "[JavaPackage " + name + "]";
    }

    @Override
    public Object getDefaultValue(final Class<?> hint) {
        if (hint == String.class) {
            return toString();
        }

        return super.getDefaultValue(hint);
    }

    /**
     * "No such property" call placeholder.
     *
     * This can never be called as we override {@link ScriptObject#noSuchProperty}. We do declare it here as it's a signal
     * to {@link WithObject} that it's worth trying doing a {@code noSuchProperty} on this object.
     *
     * @param self self reference
     * @param name property name
     * @return never returns
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE)
    public static Object __noSuchProperty__(final Object self, final Object name) {
        throw new AssertionError("__noSuchProperty__ placeholder called");
    }

    /**
     * "No such method call" placeholder
     *
     * This can never be called as we override {@link ScriptObject#noSuchMethod}. We do declare it here as it's a signal
     * to {@link WithObject} that it's worth trying doing a noSuchProperty on this object.
     *
     * @param self self reference
     * @param args arguments to method
     * @return never returns
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE)
    public static Object __noSuchMethod__(final Object self, final Object... args) {
        throw new AssertionError("__noSuchMethod__ placeholder called");
    }

    /**
     * Handle creation of new attribute.
     * @param desc the call site descriptor
     * @param request the link request
     * @return Link to be invoked at call site.
     */
    @Override
    public GuardedInvocation noSuchProperty(final CallSiteDescriptor desc, final LinkRequest request) {
        final String propertyName = desc.getNameToken(2);
        final String fullName     = name.isEmpty() ? propertyName : name + "." + propertyName;

        final Context context = getContext();
        final boolean strict  = context._strict;

        Class<?> javaClass = null;
        try {
            javaClass = context.findClass(fullName);
        } catch (final ClassNotFoundException e) {
            //ignored
        }

        if (javaClass == null) {
            set(propertyName, new NativeJavaPackage(fullName, getProto()), strict);
        } else {
            set(propertyName, StaticClass.forClass(javaClass), strict);
        }

        return super.lookup(desc, request);
    }

    @Override
    public GuardedInvocation noSuchMethod(final CallSiteDescriptor desc, final LinkRequest request) {
        return noSuchProperty(desc, request);
    }

}
