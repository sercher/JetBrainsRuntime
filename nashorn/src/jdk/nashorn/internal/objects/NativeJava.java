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

package jdk.nashorn.internal.objects;

import static jdk.nashorn.internal.runtime.ECMAErrors.typeError;
import static jdk.nashorn.internal.runtime.ScriptRuntime.UNDEFINED;

import java.lang.reflect.Array;
import java.util.Collection;
import jdk.internal.dynalink.beans.StaticClass;
import jdk.internal.dynalink.support.TypeUtilities;
import jdk.nashorn.internal.objects.annotations.Attribute;
import jdk.nashorn.internal.objects.annotations.Function;
import jdk.nashorn.internal.objects.annotations.ScriptClass;
import jdk.nashorn.internal.objects.annotations.Where;
import jdk.nashorn.internal.runtime.JSType;
import jdk.nashorn.internal.runtime.ScriptObject;
import jdk.nashorn.internal.runtime.linker.JavaAdapterFactory;

/**
 * This class is the implementation for the {@code Java} global object exposed to programs running under Nashorn. This
 * object acts as the API entry point to Java platform specific functionality, dealing with creating new instances of
 * Java classes, subclassing Java classes, implementing Java interfaces, converting between Java arrays and ECMAScript
 * arrays, and so forth.
 */
@ScriptClass("Java")
public final class NativeJava {

    private NativeJava() {
    }

    /**
     * Returns true if the specified object is a Java type object, that is an instance of {@link StaticClass}.
     * @param self not used
     * @param type the object that is checked if it is a type object or not
     * @return tells whether given object is a Java type object or not.
     * @see #type(Object, Object)
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE, where = Where.CONSTRUCTOR)
    public static Object isType(final Object self, final Object type) {
        return type instanceof StaticClass;
    }

    /**
     * Given a name of a Java type, returns an object representing that type in Nashorn. The Java class of the objects
     * used to represent Java types in Nashorn is not {@link java.lang.Class} but rather {@link StaticClass}. They are
     * the objects that you can use with the {@code new} operator to create new instances of the class as well as to
     * access static members of the class. In Nashorn, {@code Class} objects are just regular Java objects that aren't
     * treated specially. Instead of them, {@link StaticClass} instances - which we sometimes refer to as "Java type
     * objects" are used as constructors with the {@code new} operator, and they expose static fields, properties, and
     * methods. While this might seem confusing at first, it actually closely matches the Java language: you use a
     * different expression (e.g. {@code java.io.File}) as an argument in "new" and to address statics, and it is
     * distinct from the {@code Class} object (e.g. {@code java.io.File.class}). Below we cover in details the
     * properties of the type objects.
     * <h2>Constructing Java objects</h2>
     * Examples:
     * <pre>
     * var arrayListType = Java.type("java.util.ArrayList")
     * var intType = Java.type("int")
     * var stringArrayType = Java.type("java.lang.String[]")
     * var int2DArrayType = Java.type("int[][]")
     * </pre>
     * Note that the name of the type is always a string for a fully qualified name. You can use any of these types to
     * create new instances, e.g.:
     * <pre>
     * var anArrayList = new Java.type("java.util.ArrayList")
     * </pre>
     * or
     * <pre>
     * var ArrayList = Java.type("java.util.ArrayList")
     * var anArrayList = new ArrayList
     * var anArrayListWithSize = new ArrayList(16)
     * </pre>
     * In the special case of inner classes, you need to use the JVM fully qualified name, meaning using {@code $} sign
     * in the class name:
     * <pre>
     * var ftype = Java.type("java.awt.geom.Arc2D$Float")
     * </pre>
     * However, once you retrieved the outer class, you can access the inner class as a property on it:
     * <pre>
     * var arctype = Java.type("java.awt.geom.Arc2D")
     * var ftype = arctype.Float
     * </pre>
     * You can access both static and non-static inner classes. If you want to create an instance of a non-static
     * inner class, remember to pass an instance of its outer class as the first argument to the constructor.
     * </p><p>
     * If the type is abstract, you can instantiate an anonymous subclass of it using an argument list that is
     * applicable to any of its public or protected constructors, but inserting a JavaScript object with functions
     * properties that provide JavaScript implementations of the abstract methods. If method names are overloaded, the
     * JavaScript function will provide implementation for all overloads. E.g.:
     * <pre>
     * var TimerTask =  Java.type("java.util.TimerTask")
     * var task = new TimerTask({ run: function() { print("Hello World!") } })
     * </pre>
     * Nashorn supports a syntactic extension where a "new" expression followed by an argument is identical to
     * invoking the constructor and passing the argument to it, so you can write the above example also as:
     * <pre>
     * var task = new TimerTask {
     *     run: function() {
     *       print("Hello World!")
     *     }
     * }
     * </pre>
     * which is very similar to Java anonymous inner class definition. On the other hand, if the type is an abstract
     * type with a single abstract method (commonly referred to as a "SAM type") or all abstract methods it has share
     * the same overloaded name), then instead of an object, you can just pass a function, so the above example can
     * become even more simplified to:
     * <pre>
     * var task = new TimerTask(function() { print("Hello World!") })
     * </pre>
     * Note that in every one of these cases if you are trying to instantiate an abstract class that has constructors
     * that take some arguments, you can invoke those simply by specifying the arguments after the initial
     * implementation object or function.
     * </p><p>The use of functions can be taken even further; if you are invoking a Java method that takes a SAM type,
     * you can just pass in a function object, and Nashorn will know what you meant:
     * <pre>
     * var timer = new Java.type("java.util.Timer")
     * timer.schedule(function() { print("Hello World!") })
     * </pre>
     * Here, {@code Timer.schedule()} expects a {@code TimerTask} as its argument, so Nashorn creates an instance of a
     * {@code TimerTask} subclass and uses the passed function to implement its only abstract method, {@code run()}. In
     * this usage though, you can't use non-default constructors; the type must be either an interface, or must have a
     * protected or public no-arg constructor.
     * </p><p>
     * You can also subclass non-abstract classes; for that you will need to use the {@link #extend(Object, Object...)}
     * method.
     * <h2>Accessing static members</h2>
     * Examples:
     * <pre>
     * var File = Java.type("java.io.File")
     * var pathSep = File.pathSeparator
     * var tmpFile1 = File.createTempFile("abcdefg", ".tmp")
     * var tmpFile2 = File.createTempFile("abcdefg", ".tmp", new File("/tmp"))
     * </pre>
     * Actually, you can even assign static methods to variables, so the above example can be rewritten as:
     * <pre>
     * var File = Java.type("java.io.File")
     * var createTempFile = File.createTempFile
     * var tmpFile1 = createTempFile("abcdefg", ".tmp")
     * var tmpFile2 = createTempFile("abcdefg", ".tmp", new File("/tmp"))
     * </pre>
     * If you need to access the actual {@code java.lang.Class} object for the type, you can use the {@code class}
     * property on the object representing the type:
     * <pre>
     * var File = Java.type("java.io.File")
     * var someFile = new File("blah")
     * print(File.class === someFile.getClass()) // prints true
     * </pre>
     * Of course, you can also use the {@code getClass()} method or its equivalent {@code class} property on any
     * instance of the class. Other way round, you can use the synthetic {@code static} property on any
     * {@code java.lang.Class} object to retrieve its type-representing object:
     * <pre>
     * var File = Java.type("java.io.File")
     * print(File.class.static === File) // prints true
     * </pre>
     * <h2>{@code instanceof} operator</h2>
     * The standard ECMAScript {@code instanceof} operator is extended to recognize Java objects and their type objects:
     * <pre>
     * var File = Java.type("java.io.File")
     * var aFile = new File("foo")
     * print(aFile instanceof File) // prints true
     * print(aFile instanceof File.class) // prints false - Class objects aren't type objects.
     * </pre>
     * @param self not used
     * @param objTypeName the object whose JS string value represents the type name. You can use names of primitive Java
     * types to obtain representations of them, and you can use trailing square brackets to represent Java array types.
     * @return the object representing the named type
     * @throws ClassNotFoundException if the class is not found
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE, where = Where.CONSTRUCTOR)
    public static Object type(final Object self, final Object objTypeName) throws ClassNotFoundException {
        return type(objTypeName);
    }

    private static StaticClass type(final Object objTypeName) throws ClassNotFoundException {
        return StaticClass.forClass(type(JSType.toString(objTypeName)));
    }

    private static Class<?> type(final String typeName) throws ClassNotFoundException {
        if (typeName.endsWith("[]")) {
            return arrayType(typeName);
        }

        return simpleType(typeName);
    }

    /**
     * Given a JavaScript array and a Java type, returns a Java array with the same initial contents, and with the
     * specified component type. Example:
     * <pre>
     * var anArray = [1, "13", false]
     * var javaIntArray = Java.toJavaArray(anArray, "int")
     * print(javaIntArray[0]) // prints 1
     * print(javaIntArray[1]) // prints 13, as string "13" was converted to number 13 as per ECMAScript ToNumber conversion
     * print(javaIntArray[2]) // prints 0, as boolean false was converted to number 0 as per ECMAScript ToNumber conversion
     * </pre>
     * @param self not used
     * @param objArray the JavaScript array. Can be null.
     * @param objType either a {@link #type(Object, Object) type object} or a String describing the component type of
     * the Java array to create. Can not be null. If undefined, Object is assumed (allowing the argument to be omitted).
     * @return a Java array with the copy of JavaScript array's contents, converted to the appropriate Java component
     * type. Returns null if objArray is null.
     * @throws ClassNotFoundException if the class described by objType is not found
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE, where = Where.CONSTRUCTOR)
    public static Object toJavaArray(final Object self, final Object objArray, final Object objType) throws ClassNotFoundException {
        final StaticClass componentType =
            objType instanceof StaticClass ?
                (StaticClass)objType :
                objType == UNDEFINED ?
                    StaticClass.forClass(Object.class) :
                    type(objType);

        if (objArray == null) {
            return null;
        }

        Global.checkObject(objArray);

        return ((ScriptObject)objArray).getArray().asArrayOfType(componentType.getRepresentedClass());
    }

    /**
     * Given a Java array or {@link Collection}, returns a JavaScript array with a shallow copy of its contents. Note
     * that in most cases, you can use Java arrays and lists natively in Nashorn; in cases where for some reason you
     * need to have an actual JavaScript native array (e.g. to work with the array comprehensions functions), you will
     * want to use this method. Example:
     * <pre>
     * var File = Java.type("java.io.File")
     * var listHomeDir = new File("~").listFiles()
     * var jsListHome = Java.toJavaScriptArray(listHomeDir)
     * var jpegModifiedDates = jsListHome
     *     .filter(function(val) { return val.getName().endsWith(".jpg") })
     *     .map(function(val) { return val.lastModified() })
     * </pre>
     * @param self not used
     * @param objArray the java array or collection. Can be null.
     * @return a JavaScript array with the copy of Java array's or collection's contents. Returns null if objArray is
     * null.
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE, where = Where.CONSTRUCTOR)
    public static Object toJavaScriptArray(final Object self, final Object objArray) {
        if (objArray == null) {
            return null;
        } else if (objArray instanceof Collection) {
            return new NativeArray(((Collection<?>)objArray).toArray());
        } else if (objArray instanceof Object[]) {
            return new NativeArray(((Object[])objArray).clone());
        } else if (objArray instanceof int[]) {
            return new NativeArray(((int[])objArray).clone());
        } else if (objArray instanceof double[]) {
            return new NativeArray(((double[])objArray).clone());
        } else if (objArray instanceof long[]) {
            return new NativeArray(((long[])objArray).clone());
        } else if (objArray instanceof byte[]) {
            return new NativeArray(copyArray((byte[])objArray));
        } else if (objArray instanceof short[]) {
            return new NativeArray(copyArray((short[])objArray));
        } else if (objArray instanceof char[]) {
            return new NativeArray(copyArray((char[])objArray));
        } else if (objArray instanceof float[]) {
            return new NativeArray(copyArray((float[])objArray));
        } else if (objArray instanceof boolean[]) {
            return new NativeArray(copyArray((boolean[])objArray));
        }

        typeError("cant.convert.to.javascript.array", objArray.getClass().getName());

        throw new AssertionError();
    }

    private static int[] copyArray(final byte[] in) {
        final int[] out = new int[in.length];
        for(int i = 0; i < in.length; ++i) {
            out[i] = in[i];
        }
        return out;
    }

    private static int[] copyArray(final short[] in) {
        final int[] out = new int[in.length];
        for(int i = 0; i < in.length; ++i) {
            out[i] = in[i];
        }
        return out;
    }

    private static int[] copyArray(final char[] in) {
        final int[] out = new int[in.length];
        for(int i = 0; i < in.length; ++i) {
            out[i] = in[i];
        }
        return out;
    }

    private static double[] copyArray(final float[] in) {
        final double[] out = new double[in.length];
        for(int i = 0; i < in.length; ++i) {
            out[i] = in[i];
        }
        return out;
    }

    private static Object[] copyArray(final boolean[] in) {
        final Object[] out = new Object[in.length];
        for(int i = 0; i < in.length; ++i) {
            out[i] = in[i];
        }
        return out;
    }

    private static Class<?> simpleType(final String typeName) throws ClassNotFoundException {
        final Class<?> primClass = TypeUtilities.getPrimitiveTypeByName(typeName);
        return primClass != null ? primClass : Global.getThisContext().findClass(typeName);
    }

    private static Class<?> arrayType(final String typeName) throws ClassNotFoundException {
        return Array.newInstance(type(typeName.substring(0, typeName.length() - 2)), 0).getClass();
    }

    /**
     * Returns a type object for a subclass of the specified Java class (or implementation of the specified interface)
     * that acts as a script-to-Java adapter for it. See {@link #type(Object, Object)} for a discussion of type objects,
     * and see {@link JavaAdapterFactory} for details on script-to-Java adapters. Note that you can also implement
     * interfaces and subclass abstract classes using {@code new} operator on a type object for an interface or abstract
     * class. However, to extend a non-abstract class, you will have to use this method. Example:
     * <pre>
     * var ArrayList = Java.type("java.util.ArrayList")
     * var ArrayListExtender = Java.extend(ArrayList)
     * var printSizeInvokedArrayList = new ArrayListExtender() {
     *     size: function() { print("size invoked!"); }
     * }
     * var printAddInvokedArrayList = new ArrayListExtender() {
     *     add: function(x, y) {
     *       if(typeof(y) === "undefined") {
     *           print("add(e) invoked!");
     *       } else {
     *           print("add(i, e) invoked!");
     *       }
     * }
     * </pre>
     * We can see several important concepts in the above example:
     * <ul>
     * <li>Every Java class will have exactly one extender subclass in Nashorn - repeated invocations of {@code extend}
     * for the same type will yield the same extender type. It's a generic adapter that delegates to whatever JavaScript
     * functions its implementation object has on a per-instance basis.</li>
     * <li>If the Java method is overloaded (as in the above example {@code List.add()}), then your JavaScript adapter
     * must be prepared to deal with all overloads.</li>
     * <li>You can't invoke {@code super.*()} from adapters for now.</li>
     * @param self not used
     * @param types the original types. The caller must pass at least one Java type object of class {@link StaticClass}
     * representing either a public interface or a non-final public class with at least one public or protected
     * constructor. If more than one type is specified, at most one can be a class and the rest have to be interfaces.
     * Invoking the method twice with exactly the same types in the same order will return the same adapter
     * class, any reordering of types or even addition or removal of redundant types (i.e. interfaces that other types
     * in the list already implement/extend, or {@code java.lang.Object} in a list of types consisting purely of
     * interfaces) will result in a different adapter class, even though those adapter classes are functionally
     * identical; we deliberately don't want to incur the additional processing cost of canonicalizing type lists.
     * @return a new {@link StaticClass} that represents the adapter for the original types.
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE, where = Where.CONSTRUCTOR)
    public static Object extend(final Object self, final Object... types) {
        if(types == null || types.length == 0) {
            typeError("extend.expects.at.least.one.argument");
            throw new AssertionError(); //circumvent warning for types == null below
        }
        final Class<?>[] stypes = new Class<?>[types.length];
        try {
            for(int i = 0; i < types.length; ++i) {
                stypes[i] = ((StaticClass)types[i]).getRepresentedClass();
            }
        } catch(final ClassCastException e) {
            typeError("extend.expects.java.types");
        }
        return JavaAdapterFactory.getAdapterClassFor(stypes);
    }
}
