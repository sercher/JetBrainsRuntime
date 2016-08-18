/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.invoke;

import jdk.internal.misc.JavaLangInvokeAccess;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.org.objectweb.asm.AnnotationVisitor;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.Reflection;
import jdk.internal.vm.annotation.ForceInline;
import jdk.internal.vm.annotation.Stable;
import sun.invoke.empty.Empty;
import sun.invoke.util.ValueConversions;
import sun.invoke.util.VerifyType;
import sun.invoke.util.Wrapper;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.lang.invoke.LambdaForm.*;
import static java.lang.invoke.MethodHandleStatics.*;
import static java.lang.invoke.MethodHandles.Lookup.IMPL_LOOKUP;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

/**
 * Trusted implementation code for MethodHandle.
 * @author jrose
 */
/*non-public*/ abstract class MethodHandleImpl {

    /// Factory methods to create method handles:

    static MethodHandle makeArrayElementAccessor(Class<?> arrayClass, ArrayAccess access) {
        if (arrayClass == Object[].class) {
            return ArrayAccess.objectAccessor(access);
        }
        if (!arrayClass.isArray())
            throw newIllegalArgumentException("not an array: "+arrayClass);
        MethodHandle[] cache = ArrayAccessor.TYPED_ACCESSORS.get(arrayClass);
        int cacheIndex = ArrayAccess.cacheIndex(access);
        MethodHandle mh = cache[cacheIndex];
        if (mh != null)  return mh;
        mh = ArrayAccessor.getAccessor(arrayClass, access);
        MethodType correctType = ArrayAccessor.correctType(arrayClass, access);
        if (mh.type() != correctType) {
            assert(mh.type().parameterType(0) == Object[].class);
            /* if access == SET */ assert(access != ArrayAccess.SET || mh.type().parameterType(2) == Object.class);
            /* if access == GET */ assert(access != ArrayAccess.GET ||
                    (mh.type().returnType() == Object.class &&
                     correctType.parameterType(0).getComponentType() == correctType.returnType()));
            // safe to view non-strictly, because element type follows from array type
            mh = mh.viewAsType(correctType, false);
        }
        mh = makeIntrinsic(mh, ArrayAccess.intrinsic(access));
        // Atomically update accessor cache.
        synchronized(cache) {
            if (cache[cacheIndex] == null) {
                cache[cacheIndex] = mh;
            } else {
                // Throw away newly constructed accessor and use cached version.
                mh = cache[cacheIndex];
            }
        }
        return mh;
    }

    enum ArrayAccess {
        GET, SET, LENGTH;

        // As ArrayAccess and ArrayAccessor have a circular dependency, the ArrayAccess properties cannot be stored in
        // final fields.

        static String opName(ArrayAccess a) {
            switch (a) {
                case GET: return "getElement";
                case SET: return "setElement";
                case LENGTH: return "length";
            }
            throw unmatchedArrayAccess(a);
        }

        static MethodHandle objectAccessor(ArrayAccess a) {
            switch (a) {
                case GET: return ArrayAccessor.OBJECT_ARRAY_GETTER;
                case SET: return ArrayAccessor.OBJECT_ARRAY_SETTER;
                case LENGTH: return ArrayAccessor.OBJECT_ARRAY_LENGTH;
            }
            throw unmatchedArrayAccess(a);
        }

        static int cacheIndex(ArrayAccess a) {
            switch (a) {
                case GET: return ArrayAccessor.GETTER_INDEX;
                case SET: return ArrayAccessor.SETTER_INDEX;
                case LENGTH: return ArrayAccessor.LENGTH_INDEX;
            }
            throw unmatchedArrayAccess(a);
        }

        static Intrinsic intrinsic(ArrayAccess a) {
            switch (a) {
                case GET: return Intrinsic.ARRAY_LOAD;
                case SET: return Intrinsic.ARRAY_STORE;
                case LENGTH: return Intrinsic.ARRAY_LENGTH;
            }
            throw unmatchedArrayAccess(a);
        }
    }

    static InternalError unmatchedArrayAccess(ArrayAccess a) {
        return newInternalError("should not reach here (unmatched ArrayAccess: " + a + ")");
    }

    static final class ArrayAccessor {
        /// Support for array element and length access
        static final int GETTER_INDEX = 0, SETTER_INDEX = 1, LENGTH_INDEX = 2, INDEX_LIMIT = 3;
        static final ClassValue<MethodHandle[]> TYPED_ACCESSORS
                = new ClassValue<MethodHandle[]>() {
                    @Override
                    protected MethodHandle[] computeValue(Class<?> type) {
                        return new MethodHandle[INDEX_LIMIT];
                    }
                };
        static final MethodHandle OBJECT_ARRAY_GETTER, OBJECT_ARRAY_SETTER, OBJECT_ARRAY_LENGTH;
        static {
            MethodHandle[] cache = TYPED_ACCESSORS.get(Object[].class);
            cache[GETTER_INDEX] = OBJECT_ARRAY_GETTER = makeIntrinsic(getAccessor(Object[].class, ArrayAccess.GET),    Intrinsic.ARRAY_LOAD);
            cache[SETTER_INDEX] = OBJECT_ARRAY_SETTER = makeIntrinsic(getAccessor(Object[].class, ArrayAccess.SET),    Intrinsic.ARRAY_STORE);
            cache[LENGTH_INDEX] = OBJECT_ARRAY_LENGTH = makeIntrinsic(getAccessor(Object[].class, ArrayAccess.LENGTH), Intrinsic.ARRAY_LENGTH);

            assert(InvokerBytecodeGenerator.isStaticallyInvocable(ArrayAccessor.OBJECT_ARRAY_GETTER.internalMemberName()));
            assert(InvokerBytecodeGenerator.isStaticallyInvocable(ArrayAccessor.OBJECT_ARRAY_SETTER.internalMemberName()));
            assert(InvokerBytecodeGenerator.isStaticallyInvocable(ArrayAccessor.OBJECT_ARRAY_LENGTH.internalMemberName()));
        }

        static int     getElementI(int[]     a, int i)            { return              a[i]; }
        static long    getElementJ(long[]    a, int i)            { return              a[i]; }
        static float   getElementF(float[]   a, int i)            { return              a[i]; }
        static double  getElementD(double[]  a, int i)            { return              a[i]; }
        static boolean getElementZ(boolean[] a, int i)            { return              a[i]; }
        static byte    getElementB(byte[]    a, int i)            { return              a[i]; }
        static short   getElementS(short[]   a, int i)            { return              a[i]; }
        static char    getElementC(char[]    a, int i)            { return              a[i]; }
        static Object  getElementL(Object[]  a, int i)            { return              a[i]; }

        static void    setElementI(int[]     a, int i, int     x) {              a[i] = x; }
        static void    setElementJ(long[]    a, int i, long    x) {              a[i] = x; }
        static void    setElementF(float[]   a, int i, float   x) {              a[i] = x; }
        static void    setElementD(double[]  a, int i, double  x) {              a[i] = x; }
        static void    setElementZ(boolean[] a, int i, boolean x) {              a[i] = x; }
        static void    setElementB(byte[]    a, int i, byte    x) {              a[i] = x; }
        static void    setElementS(short[]   a, int i, short   x) {              a[i] = x; }
        static void    setElementC(char[]    a, int i, char    x) {              a[i] = x; }
        static void    setElementL(Object[]  a, int i, Object  x) {              a[i] = x; }

        static int     lengthI(int[]     a)                       { return a.length; }
        static int     lengthJ(long[]    a)                       { return a.length; }
        static int     lengthF(float[]   a)                       { return a.length; }
        static int     lengthD(double[]  a)                       { return a.length; }
        static int     lengthZ(boolean[] a)                       { return a.length; }
        static int     lengthB(byte[]    a)                       { return a.length; }
        static int     lengthS(short[]   a)                       { return a.length; }
        static int     lengthC(char[]    a)                       { return a.length; }
        static int     lengthL(Object[]  a)                       { return a.length; }

        static String name(Class<?> arrayClass, ArrayAccess access) {
            Class<?> elemClass = arrayClass.getComponentType();
            if (elemClass == null)  throw newIllegalArgumentException("not an array", arrayClass);
            return ArrayAccess.opName(access) + Wrapper.basicTypeChar(elemClass);
        }
        static MethodType type(Class<?> arrayClass, ArrayAccess access) {
            Class<?> elemClass = arrayClass.getComponentType();
            Class<?> arrayArgClass = arrayClass;
            if (!elemClass.isPrimitive()) {
                arrayArgClass = Object[].class;
                elemClass = Object.class;
            }
            switch (access) {
                case GET:    return MethodType.methodType(elemClass,  arrayArgClass, int.class);
                case SET:    return MethodType.methodType(void.class, arrayArgClass, int.class, elemClass);
                case LENGTH: return MethodType.methodType(int.class,  arrayArgClass);
            }
            throw unmatchedArrayAccess(access);
        }
        static MethodType correctType(Class<?> arrayClass, ArrayAccess access) {
            Class<?> elemClass = arrayClass.getComponentType();
            switch (access) {
                case GET:    return MethodType.methodType(elemClass,  arrayClass, int.class);
                case SET:    return MethodType.methodType(void.class, arrayClass, int.class, elemClass);
                case LENGTH: return MethodType.methodType(int.class,  arrayClass);
            }
            throw unmatchedArrayAccess(access);
        }
        static MethodHandle getAccessor(Class<?> arrayClass, ArrayAccess access) {
            String     name = name(arrayClass, access);
            MethodType type = type(arrayClass, access);
            try {
                return IMPL_LOOKUP.findStatic(ArrayAccessor.class, name, type);
            } catch (ReflectiveOperationException ex) {
                throw uncaughtException(ex);
            }
        }
    }

    /**
     * Create a JVM-level adapter method handle to conform the given method
     * handle to the similar newType, using only pairwise argument conversions.
     * For each argument, convert incoming argument to the exact type needed.
     * The argument conversions allowed are casting, boxing and unboxing,
     * integral widening or narrowing, and floating point widening or narrowing.
     * @param srcType required call type
     * @param target original method handle
     * @param strict if true, only asType conversions are allowed; if false, explicitCastArguments conversions allowed
     * @param monobox if true, unboxing conversions are assumed to be exactly typed (Integer to int only, not long or double)
     * @return an adapter to the original handle with the desired new type,
     *          or the original target if the types are already identical
     *          or null if the adaptation cannot be made
     */
    static MethodHandle makePairwiseConvert(MethodHandle target, MethodType srcType,
                                            boolean strict, boolean monobox) {
        MethodType dstType = target.type();
        if (srcType == dstType)
            return target;
        return makePairwiseConvertByEditor(target, srcType, strict, monobox);
    }

    private static int countNonNull(Object[] array) {
        int count = 0;
        for (Object x : array) {
            if (x != null)  ++count;
        }
        return count;
    }

    static MethodHandle makePairwiseConvertByEditor(MethodHandle target, MethodType srcType,
                                                    boolean strict, boolean monobox) {
        Object[] convSpecs = computeValueConversions(srcType, target.type(), strict, monobox);
        int convCount = countNonNull(convSpecs);
        if (convCount == 0)
            return target.viewAsType(srcType, strict);
        MethodType basicSrcType = srcType.basicType();
        MethodType midType = target.type().basicType();
        BoundMethodHandle mh = target.rebind();
        // FIXME: Reduce number of bindings when there is more than one Class conversion.
        // FIXME: Reduce number of bindings when there are repeated conversions.
        for (int i = 0; i < convSpecs.length-1; i++) {
            Object convSpec = convSpecs[i];
            if (convSpec == null)  continue;
            MethodHandle fn;
            if (convSpec instanceof Class) {
                fn = getConstantHandle(MH_cast).bindTo(convSpec);
            } else {
                fn = (MethodHandle) convSpec;
            }
            Class<?> newType = basicSrcType.parameterType(i);
            if (--convCount == 0)
                midType = srcType;
            else
                midType = midType.changeParameterType(i, newType);
            LambdaForm form2 = mh.editor().filterArgumentForm(1+i, BasicType.basicType(newType));
            mh = mh.copyWithExtendL(midType, form2, fn);
            mh = mh.rebind();
        }
        Object convSpec = convSpecs[convSpecs.length-1];
        if (convSpec != null) {
            MethodHandle fn;
            if (convSpec instanceof Class) {
                if (convSpec == void.class)
                    fn = null;
                else
                    fn = getConstantHandle(MH_cast).bindTo(convSpec);
            } else {
                fn = (MethodHandle) convSpec;
            }
            Class<?> newType = basicSrcType.returnType();
            assert(--convCount == 0);
            midType = srcType;
            if (fn != null) {
                mh = mh.rebind();  // rebind if too complex
                LambdaForm form2 = mh.editor().filterReturnForm(BasicType.basicType(newType), false);
                mh = mh.copyWithExtendL(midType, form2, fn);
            } else {
                LambdaForm form2 = mh.editor().filterReturnForm(BasicType.basicType(newType), true);
                mh = mh.copyWith(midType, form2);
            }
        }
        assert(convCount == 0);
        assert(mh.type().equals(srcType));
        return mh;
    }

    static MethodHandle makePairwiseConvertIndirect(MethodHandle target, MethodType srcType,
                                                    boolean strict, boolean monobox) {
        assert(target.type().parameterCount() == srcType.parameterCount());
        // Calculate extra arguments (temporaries) required in the names array.
        Object[] convSpecs = computeValueConversions(srcType, target.type(), strict, monobox);
        final int INARG_COUNT = srcType.parameterCount();
        int convCount = countNonNull(convSpecs);
        boolean retConv = (convSpecs[INARG_COUNT] != null);
        boolean retVoid = srcType.returnType() == void.class;
        if (retConv && retVoid) {
            convCount -= 1;
            retConv = false;
        }

        final int IN_MH         = 0;
        final int INARG_BASE    = 1;
        final int INARG_LIMIT   = INARG_BASE + INARG_COUNT;
        final int NAME_LIMIT    = INARG_LIMIT + convCount + 1;
        final int RETURN_CONV   = (!retConv ? -1         : NAME_LIMIT - 1);
        final int OUT_CALL      = (!retConv ? NAME_LIMIT : RETURN_CONV) - 1;
        final int RESULT        = (retVoid ? -1 : NAME_LIMIT - 1);

        // Now build a LambdaForm.
        MethodType lambdaType = srcType.basicType().invokerType();
        Name[] names = arguments(NAME_LIMIT - INARG_LIMIT, lambdaType);

        // Collect the arguments to the outgoing call, maybe with conversions:
        final int OUTARG_BASE = 0;  // target MH is Name.function, name Name.arguments[0]
        Object[] outArgs = new Object[OUTARG_BASE + INARG_COUNT];

        int nameCursor = INARG_LIMIT;
        for (int i = 0; i < INARG_COUNT; i++) {
            Object convSpec = convSpecs[i];
            if (convSpec == null) {
                // do nothing: difference is trivial
                outArgs[OUTARG_BASE + i] = names[INARG_BASE + i];
                continue;
            }

            Name conv;
            if (convSpec instanceof Class) {
                Class<?> convClass = (Class<?>) convSpec;
                conv = new Name(getConstantHandle(MH_cast), convClass, names[INARG_BASE + i]);
            } else {
                MethodHandle fn = (MethodHandle) convSpec;
                conv = new Name(fn, names[INARG_BASE + i]);
            }
            assert(names[nameCursor] == null);
            names[nameCursor++] = conv;
            assert(outArgs[OUTARG_BASE + i] == null);
            outArgs[OUTARG_BASE + i] = conv;
        }

        // Build argument array for the call.
        assert(nameCursor == OUT_CALL);
        names[OUT_CALL] = new Name(target, outArgs);

        Object convSpec = convSpecs[INARG_COUNT];
        if (!retConv) {
            assert(OUT_CALL == names.length-1);
        } else {
            Name conv;
            if (convSpec == void.class) {
                conv = new Name(LambdaForm.constantZero(BasicType.basicType(srcType.returnType())));
            } else if (convSpec instanceof Class) {
                Class<?> convClass = (Class<?>) convSpec;
                conv = new Name(getConstantHandle(MH_cast), convClass, names[OUT_CALL]);
            } else {
                MethodHandle fn = (MethodHandle) convSpec;
                if (fn.type().parameterCount() == 0)
                    conv = new Name(fn);  // don't pass retval to void conversion
                else
                    conv = new Name(fn, names[OUT_CALL]);
            }
            assert(names[RETURN_CONV] == null);
            names[RETURN_CONV] = conv;
            assert(RETURN_CONV == names.length-1);
        }

        LambdaForm form = new LambdaForm("convert", lambdaType.parameterCount(), names, RESULT);
        return SimpleMethodHandle.make(srcType, form);
    }

    static Object[] computeValueConversions(MethodType srcType, MethodType dstType,
                                            boolean strict, boolean monobox) {
        final int INARG_COUNT = srcType.parameterCount();
        Object[] convSpecs = new Object[INARG_COUNT+1];
        for (int i = 0; i <= INARG_COUNT; i++) {
            boolean isRet = (i == INARG_COUNT);
            Class<?> src = isRet ? dstType.returnType() : srcType.parameterType(i);
            Class<?> dst = isRet ? srcType.returnType() : dstType.parameterType(i);
            if (!VerifyType.isNullConversion(src, dst, /*keepInterfaces=*/ strict)) {
                convSpecs[i] = valueConversion(src, dst, strict, monobox);
            }
        }
        return convSpecs;
    }
    static MethodHandle makePairwiseConvert(MethodHandle target, MethodType srcType,
                                            boolean strict) {
        return makePairwiseConvert(target, srcType, strict, /*monobox=*/ false);
    }

    /**
     * Find a conversion function from the given source to the given destination.
     * This conversion function will be used as a LF NamedFunction.
     * Return a Class object if a simple cast is needed.
     * Return void.class if void is involved.
     */
    static Object valueConversion(Class<?> src, Class<?> dst, boolean strict, boolean monobox) {
        assert(!VerifyType.isNullConversion(src, dst, /*keepInterfaces=*/ strict));  // caller responsibility
        if (dst == void.class)
            return dst;
        MethodHandle fn;
        if (src.isPrimitive()) {
            if (src == void.class) {
                return void.class;  // caller must recognize this specially
            } else if (dst.isPrimitive()) {
                // Examples: int->byte, byte->int, boolean->int (!strict)
                fn = ValueConversions.convertPrimitive(src, dst);
            } else {
                // Examples: int->Integer, boolean->Object, float->Number
                Wrapper wsrc = Wrapper.forPrimitiveType(src);
                fn = ValueConversions.boxExact(wsrc);
                assert(fn.type().parameterType(0) == wsrc.primitiveType());
                assert(fn.type().returnType() == wsrc.wrapperType());
                if (!VerifyType.isNullConversion(wsrc.wrapperType(), dst, strict)) {
                    // Corner case, such as int->Long, which will probably fail.
                    MethodType mt = MethodType.methodType(dst, src);
                    if (strict)
                        fn = fn.asType(mt);
                    else
                        fn = MethodHandleImpl.makePairwiseConvert(fn, mt, /*strict=*/ false);
                }
            }
        } else if (dst.isPrimitive()) {
            Wrapper wdst = Wrapper.forPrimitiveType(dst);
            if (monobox || src == wdst.wrapperType()) {
                // Use a strongly-typed unboxer, if possible.
                fn = ValueConversions.unboxExact(wdst, strict);
            } else {
                // Examples:  Object->int, Number->int, Comparable->int, Byte->int
                // must include additional conversions
                // src must be examined at runtime, to detect Byte, Character, etc.
                fn = (strict
                        ? ValueConversions.unboxWiden(wdst)
                        : ValueConversions.unboxCast(wdst));
            }
        } else {
            // Simple reference conversion.
            // Note:  Do not check for a class hierarchy relation
            // between src and dst.  In all cases a 'null' argument
            // will pass the cast conversion.
            return dst;
        }
        assert(fn.type().parameterCount() <= 1) : "pc"+Arrays.asList(src.getSimpleName(), dst.getSimpleName(), fn);
        return fn;
    }

    static MethodHandle makeVarargsCollector(MethodHandle target, Class<?> arrayType) {
        MethodType type = target.type();
        int last = type.parameterCount() - 1;
        if (type.parameterType(last) != arrayType)
            target = target.asType(type.changeParameterType(last, arrayType));
        target = target.asFixedArity();  // make sure this attribute is turned off
        return new AsVarargsCollector(target, arrayType);
    }

    private static final class AsVarargsCollector extends DelegatingMethodHandle {
        private final MethodHandle target;
        private final Class<?> arrayType;
        private @Stable MethodHandle asCollectorCache;

        AsVarargsCollector(MethodHandle target, Class<?> arrayType) {
            this(target.type(), target, arrayType);
        }
        AsVarargsCollector(MethodType type, MethodHandle target, Class<?> arrayType) {
            super(type, target);
            this.target = target;
            this.arrayType = arrayType;
            this.asCollectorCache = target.asCollector(arrayType, 0);
        }

        @Override
        public boolean isVarargsCollector() {
            return true;
        }

        @Override
        protected MethodHandle getTarget() {
            return target;
        }

        @Override
        public MethodHandle asFixedArity() {
            return target;
        }

        @Override
        MethodHandle setVarargs(MemberName member) {
            if (member.isVarargs())  return this;
            return asFixedArity();
        }

        @Override
        public MethodHandle asTypeUncached(MethodType newType) {
            MethodType type = this.type();
            int collectArg = type.parameterCount() - 1;
            int newArity = newType.parameterCount();
            if (newArity == collectArg+1 &&
                type.parameterType(collectArg).isAssignableFrom(newType.parameterType(collectArg))) {
                // if arity and trailing parameter are compatible, do normal thing
                return asTypeCache = asFixedArity().asType(newType);
            }
            // check cache
            MethodHandle acc = asCollectorCache;
            if (acc != null && acc.type().parameterCount() == newArity)
                return asTypeCache = acc.asType(newType);
            // build and cache a collector
            int arrayLength = newArity - collectArg;
            MethodHandle collector;
            try {
                collector = asFixedArity().asCollector(arrayType, arrayLength);
                assert(collector.type().parameterCount() == newArity) : "newArity="+newArity+" but collector="+collector;
            } catch (IllegalArgumentException ex) {
                throw new WrongMethodTypeException("cannot build collector", ex);
            }
            asCollectorCache = collector;
            return asTypeCache = collector.asType(newType);
        }

        @Override
        boolean viewAsTypeChecks(MethodType newType, boolean strict) {
            super.viewAsTypeChecks(newType, true);
            if (strict) return true;
            // extra assertion for non-strict checks:
            assert (type().lastParameterType().getComponentType()
                    .isAssignableFrom(
                            newType.lastParameterType().getComponentType()))
                    : Arrays.asList(this, newType);
            return true;
        }
    }

    /** Factory method:  Spread selected argument. */
    static MethodHandle makeSpreadArguments(MethodHandle target,
                                            Class<?> spreadArgType, int spreadArgPos, int spreadArgCount) {
        MethodType targetType = target.type();

        for (int i = 0; i < spreadArgCount; i++) {
            Class<?> arg = VerifyType.spreadArgElementType(spreadArgType, i);
            if (arg == null)  arg = Object.class;
            targetType = targetType.changeParameterType(spreadArgPos + i, arg);
        }
        target = target.asType(targetType);

        MethodType srcType = targetType
                .replaceParameterTypes(spreadArgPos, spreadArgPos + spreadArgCount, spreadArgType);
        // Now build a LambdaForm.
        MethodType lambdaType = srcType.invokerType();
        Name[] names = arguments(spreadArgCount + 2, lambdaType);
        int nameCursor = lambdaType.parameterCount();
        int[] indexes = new int[targetType.parameterCount()];

        for (int i = 0, argIndex = 1; i < targetType.parameterCount() + 1; i++, argIndex++) {
            Class<?> src = lambdaType.parameterType(i);
            if (i == spreadArgPos) {
                // Spread the array.
                MethodHandle aload = MethodHandles.arrayElementGetter(spreadArgType);
                Name array = names[argIndex];
                names[nameCursor++] = new Name(NF_checkSpreadArgument, array, spreadArgCount);
                for (int j = 0; j < spreadArgCount; i++, j++) {
                    indexes[i] = nameCursor;
                    names[nameCursor++] = new Name(aload, array, j);
                }
            } else if (i < indexes.length) {
                indexes[i] = argIndex;
            }
        }
        assert(nameCursor == names.length-1);  // leave room for the final call

        // Build argument array for the call.
        Name[] targetArgs = new Name[targetType.parameterCount()];
        for (int i = 0; i < targetType.parameterCount(); i++) {
            int idx = indexes[i];
            targetArgs[i] = names[idx];
        }
        names[names.length - 1] = new Name(target, (Object[]) targetArgs);

        LambdaForm form = new LambdaForm("spread", lambdaType.parameterCount(), names);
        return SimpleMethodHandle.make(srcType, form);
    }

    static void checkSpreadArgument(Object av, int n) {
        if (av == null) {
            if (n == 0)  return;
        } else if (av instanceof Object[]) {
            int len = ((Object[])av).length;
            if (len == n)  return;
        } else {
            int len = java.lang.reflect.Array.getLength(av);
            if (len == n)  return;
        }
        // fall through to error:
        throw newIllegalArgumentException("array is not of length "+n);
    }

    /** Factory method:  Collect or filter selected argument(s). */
    static MethodHandle makeCollectArguments(MethodHandle target,
                MethodHandle collector, int collectArgPos, boolean retainOriginalArgs) {
        MethodType targetType = target.type();          // (a..., c, [b...])=>r
        MethodType collectorType = collector.type();    // (b...)=>c
        int collectArgCount = collectorType.parameterCount();
        Class<?> collectValType = collectorType.returnType();
        int collectValCount = (collectValType == void.class ? 0 : 1);
        MethodType srcType = targetType                 // (a..., [b...])=>r
                .dropParameterTypes(collectArgPos, collectArgPos+collectValCount);
        if (!retainOriginalArgs) {                      // (a..., b...)=>r
            srcType = srcType.insertParameterTypes(collectArgPos, collectorType.parameterArray());
        }
        // in  arglist: [0: ...keep1 | cpos: collect...  | cpos+cacount: keep2... ]
        // out arglist: [0: ...keep1 | cpos: collectVal? | cpos+cvcount: keep2... ]
        // out(retain): [0: ...keep1 | cpos: cV? coll... | cpos+cvc+cac: keep2... ]

        // Now build a LambdaForm.
        MethodType lambdaType = srcType.invokerType();
        Name[] names = arguments(2, lambdaType);
        final int collectNamePos = names.length - 2;
        final int targetNamePos  = names.length - 1;

        Name[] collectorArgs = Arrays.copyOfRange(names, 1 + collectArgPos, 1 + collectArgPos + collectArgCount);
        names[collectNamePos] = new Name(collector, (Object[]) collectorArgs);

        // Build argument array for the target.
        // Incoming LF args to copy are: [ (mh) headArgs collectArgs tailArgs ].
        // Output argument array is [ headArgs (collectVal)? (collectArgs)? tailArgs ].
        Name[] targetArgs = new Name[targetType.parameterCount()];
        int inputArgPos  = 1;  // incoming LF args to copy to target
        int targetArgPos = 0;  // fill pointer for targetArgs
        int chunk = collectArgPos;  // |headArgs|
        System.arraycopy(names, inputArgPos, targetArgs, targetArgPos, chunk);
        inputArgPos  += chunk;
        targetArgPos += chunk;
        if (collectValType != void.class) {
            targetArgs[targetArgPos++] = names[collectNamePos];
        }
        chunk = collectArgCount;
        if (retainOriginalArgs) {
            System.arraycopy(names, inputArgPos, targetArgs, targetArgPos, chunk);
            targetArgPos += chunk;   // optionally pass on the collected chunk
        }
        inputArgPos += chunk;
        chunk = targetArgs.length - targetArgPos;  // all the rest
        System.arraycopy(names, inputArgPos, targetArgs, targetArgPos, chunk);
        assert(inputArgPos + chunk == collectNamePos);  // use of rest of input args also
        names[targetNamePos] = new Name(target, (Object[]) targetArgs);

        LambdaForm form = new LambdaForm("collect", lambdaType.parameterCount(), names);
        return SimpleMethodHandle.make(srcType, form);
    }

    @LambdaForm.Hidden
    static
    MethodHandle selectAlternative(boolean testResult, MethodHandle target, MethodHandle fallback) {
        if (testResult) {
            return target;
        } else {
            return fallback;
        }
    }

    // Intrinsified by C2. Counters are used during parsing to calculate branch frequencies.
    @LambdaForm.Hidden
    @jdk.internal.HotSpotIntrinsicCandidate
    static
    boolean profileBoolean(boolean result, int[] counters) {
        // Profile is int[2] where [0] and [1] correspond to false and true occurrences respectively.
        int idx = result ? 1 : 0;
        try {
            counters[idx] = Math.addExact(counters[idx], 1);
        } catch (ArithmeticException e) {
            // Avoid continuous overflow by halving the problematic count.
            counters[idx] = counters[idx] / 2;
        }
        return result;
    }

    // Intrinsified by C2. Returns true if obj is a compile-time constant.
    @LambdaForm.Hidden
    @jdk.internal.HotSpotIntrinsicCandidate
    static
    boolean isCompileConstant(Object obj) {
        return false;
    }

    static
    MethodHandle makeGuardWithTest(MethodHandle test,
                                   MethodHandle target,
                                   MethodHandle fallback) {
        MethodType type = target.type();
        assert(test.type().equals(type.changeReturnType(boolean.class)) && fallback.type().equals(type));
        MethodType basicType = type.basicType();
        LambdaForm form = makeGuardWithTestForm(basicType);
        BoundMethodHandle mh;
        try {
            if (PROFILE_GWT) {
                int[] counts = new int[2];
                mh = (BoundMethodHandle)
                        BoundMethodHandle.speciesData_LLLL().constructor().invokeBasic(type, form,
                                (Object) test, (Object) profile(target), (Object) profile(fallback), counts);
            } else {
                mh = (BoundMethodHandle)
                        BoundMethodHandle.speciesData_LLL().constructor().invokeBasic(type, form,
                                (Object) test, (Object) profile(target), (Object) profile(fallback));
            }
        } catch (Throwable ex) {
            throw uncaughtException(ex);
        }
        assert(mh.type() == type);
        return mh;
    }


    static
    MethodHandle profile(MethodHandle target) {
        if (DONT_INLINE_THRESHOLD >= 0) {
            return makeBlockInliningWrapper(target);
        } else {
            return target;
        }
    }

    /**
     * Block inlining during JIT-compilation of a target method handle if it hasn't been invoked enough times.
     * Corresponding LambdaForm has @DontInline when compiled into bytecode.
     */
    static
    MethodHandle makeBlockInliningWrapper(MethodHandle target) {
        LambdaForm lform;
        if (DONT_INLINE_THRESHOLD > 0) {
            lform = Makers.PRODUCE_BLOCK_INLINING_FORM.apply(target);
        } else {
            lform = Makers.PRODUCE_REINVOKER_FORM.apply(target);
        }
        return new CountingWrapper(target, lform,
                Makers.PRODUCE_BLOCK_INLINING_FORM, Makers.PRODUCE_REINVOKER_FORM,
                                   DONT_INLINE_THRESHOLD);
    }

    private final static class Makers {
        /** Constructs reinvoker lambda form which block inlining during JIT-compilation for a particular method handle */
        static final Function<MethodHandle, LambdaForm> PRODUCE_BLOCK_INLINING_FORM = new Function<MethodHandle, LambdaForm>() {
            @Override
            public LambdaForm apply(MethodHandle target) {
                return DelegatingMethodHandle.makeReinvokerForm(target,
                                   MethodTypeForm.LF_DELEGATE_BLOCK_INLINING, CountingWrapper.class, "reinvoker.dontInline", false,
                                   DelegatingMethodHandle.NF_getTarget, CountingWrapper.NF_maybeStopCounting);
            }
        };

        /** Constructs simple reinvoker lambda form for a particular method handle */
        static final Function<MethodHandle, LambdaForm> PRODUCE_REINVOKER_FORM = new Function<MethodHandle, LambdaForm>() {
            @Override
            public LambdaForm apply(MethodHandle target) {
                return DelegatingMethodHandle.makeReinvokerForm(target,
                        MethodTypeForm.LF_DELEGATE, DelegatingMethodHandle.class, DelegatingMethodHandle.NF_getTarget);
            }
        };

        /** Maker of type-polymorphic varargs */
        static final ClassValue<MethodHandle[]> TYPED_COLLECTORS = new ClassValue<MethodHandle[]>() {
            @Override
            protected MethodHandle[] computeValue(Class<?> type) {
                return new MethodHandle[MAX_JVM_ARITY + 1];
            }
        };
    }

    /**
     * Counting method handle. It has 2 states: counting and non-counting.
     * It is in counting state for the first n invocations and then transitions to non-counting state.
     * Behavior in counting and non-counting states is determined by lambda forms produced by
     * countingFormProducer & nonCountingFormProducer respectively.
     */
    static class CountingWrapper extends DelegatingMethodHandle {
        private final MethodHandle target;
        private int count;
        private Function<MethodHandle, LambdaForm> countingFormProducer;
        private Function<MethodHandle, LambdaForm> nonCountingFormProducer;
        private volatile boolean isCounting;

        private CountingWrapper(MethodHandle target, LambdaForm lform,
                                Function<MethodHandle, LambdaForm> countingFromProducer,
                                Function<MethodHandle, LambdaForm> nonCountingFormProducer,
                                int count) {
            super(target.type(), lform);
            this.target = target;
            this.count = count;
            this.countingFormProducer = countingFromProducer;
            this.nonCountingFormProducer = nonCountingFormProducer;
            this.isCounting = (count > 0);
        }

        @Hidden
        @Override
        protected MethodHandle getTarget() {
            return target;
        }

        @Override
        public MethodHandle asTypeUncached(MethodType newType) {
            MethodHandle newTarget = target.asType(newType);
            MethodHandle wrapper;
            if (isCounting) {
                LambdaForm lform;
                lform = countingFormProducer.apply(newTarget);
                wrapper = new CountingWrapper(newTarget, lform, countingFormProducer, nonCountingFormProducer, DONT_INLINE_THRESHOLD);
            } else {
                wrapper = newTarget; // no need for a counting wrapper anymore
            }
            return (asTypeCache = wrapper);
        }

        // Customize target if counting happens for too long.
        private int invocations = CUSTOMIZE_THRESHOLD;
        private void maybeCustomizeTarget() {
            int c = invocations;
            if (c >= 0) {
                if (c == 1) {
                    target.customize();
                }
                invocations = c - 1;
            }
        }

        boolean countDown() {
            int c = count;
            maybeCustomizeTarget();
            if (c <= 1) {
                // Try to limit number of updates. MethodHandle.updateForm() doesn't guarantee LF update visibility.
                if (isCounting) {
                    isCounting = false;
                    return true;
                } else {
                    return false;
                }
            } else {
                count = c - 1;
                return false;
            }
        }

        @Hidden
        static void maybeStopCounting(Object o1) {
             CountingWrapper wrapper = (CountingWrapper) o1;
             if (wrapper.countDown()) {
                 // Reached invocation threshold. Replace counting behavior with a non-counting one.
                 LambdaForm lform = wrapper.nonCountingFormProducer.apply(wrapper.target);
                 lform.compileToBytecode(); // speed up warmup by avoiding LF interpretation again after transition
                 wrapper.updateForm(lform);
             }
        }

        static final NamedFunction NF_maybeStopCounting;
        static {
            Class<?> THIS_CLASS = CountingWrapper.class;
            try {
                NF_maybeStopCounting = new NamedFunction(THIS_CLASS.getDeclaredMethod("maybeStopCounting", Object.class));
            } catch (ReflectiveOperationException ex) {
                throw newInternalError(ex);
            }
        }
    }

    static
    LambdaForm makeGuardWithTestForm(MethodType basicType) {
        LambdaForm lform = basicType.form().cachedLambdaForm(MethodTypeForm.LF_GWT);
        if (lform != null)  return lform;
        final int THIS_MH      = 0;  // the BMH_LLL
        final int ARG_BASE     = 1;  // start of incoming arguments
        final int ARG_LIMIT    = ARG_BASE + basicType.parameterCount();
        int nameCursor = ARG_LIMIT;
        final int GET_TEST     = nameCursor++;
        final int GET_TARGET   = nameCursor++;
        final int GET_FALLBACK = nameCursor++;
        final int GET_COUNTERS = PROFILE_GWT ? nameCursor++ : -1;
        final int CALL_TEST    = nameCursor++;
        final int PROFILE      = (GET_COUNTERS != -1) ? nameCursor++ : -1;
        final int TEST         = nameCursor-1; // previous statement: either PROFILE or CALL_TEST
        final int SELECT_ALT   = nameCursor++;
        final int CALL_TARGET  = nameCursor++;
        assert(CALL_TARGET == SELECT_ALT+1);  // must be true to trigger IBG.emitSelectAlternative

        MethodType lambdaType = basicType.invokerType();
        Name[] names = arguments(nameCursor - ARG_LIMIT, lambdaType);

        BoundMethodHandle.SpeciesData data =
                (GET_COUNTERS != -1) ? BoundMethodHandle.speciesData_LLLL()
                                     : BoundMethodHandle.speciesData_LLL();
        names[THIS_MH] = names[THIS_MH].withConstraint(data);
        names[GET_TEST]     = new Name(data.getterFunction(0), names[THIS_MH]);
        names[GET_TARGET]   = new Name(data.getterFunction(1), names[THIS_MH]);
        names[GET_FALLBACK] = new Name(data.getterFunction(2), names[THIS_MH]);
        if (GET_COUNTERS != -1) {
            names[GET_COUNTERS] = new Name(data.getterFunction(3), names[THIS_MH]);
        }
        Object[] invokeArgs = Arrays.copyOfRange(names, 0, ARG_LIMIT, Object[].class);

        // call test
        MethodType testType = basicType.changeReturnType(boolean.class).basicType();
        invokeArgs[0] = names[GET_TEST];
        names[CALL_TEST] = new Name(testType, invokeArgs);

        // profile branch
        if (PROFILE != -1) {
            names[PROFILE] = new Name(NF_profileBoolean, names[CALL_TEST], names[GET_COUNTERS]);
        }
        // call selectAlternative
        names[SELECT_ALT] = new Name(getConstantHandle(MH_selectAlternative), names[TEST], names[GET_TARGET], names[GET_FALLBACK]);

        // call target or fallback
        invokeArgs[0] = names[SELECT_ALT];
        names[CALL_TARGET] = new Name(basicType, invokeArgs);

        lform = new LambdaForm("guard", lambdaType.parameterCount(), names, /*forceInline=*/true);

        return basicType.form().setCachedLambdaForm(MethodTypeForm.LF_GWT, lform);
    }

    /**
     * The LambdaForm shape for catchException combinator is the following:
     * <blockquote><pre>{@code
     *  guardWithCatch=Lambda(a0:L,a1:L,a2:L)=>{
     *    t3:L=BoundMethodHandle$Species_LLLLL.argL0(a0:L);
     *    t4:L=BoundMethodHandle$Species_LLLLL.argL1(a0:L);
     *    t5:L=BoundMethodHandle$Species_LLLLL.argL2(a0:L);
     *    t6:L=BoundMethodHandle$Species_LLLLL.argL3(a0:L);
     *    t7:L=BoundMethodHandle$Species_LLLLL.argL4(a0:L);
     *    t8:L=MethodHandle.invokeBasic(t6:L,a1:L,a2:L);
     *    t9:L=MethodHandleImpl.guardWithCatch(t3:L,t4:L,t5:L,t8:L);
     *   t10:I=MethodHandle.invokeBasic(t7:L,t9:L);t10:I}
     * }</pre></blockquote>
     *
     * argL0 and argL2 are target and catcher method handles. argL1 is exception class.
     * argL3 and argL4 are auxiliary method handles: argL3 boxes arguments and wraps them into Object[]
     * (ValueConversions.array()) and argL4 unboxes result if necessary (ValueConversions.unbox()).
     *
     * Having t8 and t10 passed outside and not hardcoded into a lambda form allows to share lambda forms
     * among catchException combinators with the same basic type.
     */
    private static LambdaForm makeGuardWithCatchForm(MethodType basicType) {
        MethodType lambdaType = basicType.invokerType();

        LambdaForm lform = basicType.form().cachedLambdaForm(MethodTypeForm.LF_GWC);
        if (lform != null) {
            return lform;
        }
        final int THIS_MH      = 0;  // the BMH_LLLLL
        final int ARG_BASE     = 1;  // start of incoming arguments
        final int ARG_LIMIT    = ARG_BASE + basicType.parameterCount();

        int nameCursor = ARG_LIMIT;
        final int GET_TARGET       = nameCursor++;
        final int GET_CLASS        = nameCursor++;
        final int GET_CATCHER      = nameCursor++;
        final int GET_COLLECT_ARGS = nameCursor++;
        final int GET_UNBOX_RESULT = nameCursor++;
        final int BOXED_ARGS       = nameCursor++;
        final int TRY_CATCH        = nameCursor++;
        final int UNBOX_RESULT     = nameCursor++;

        Name[] names = arguments(nameCursor - ARG_LIMIT, lambdaType);

        BoundMethodHandle.SpeciesData data = BoundMethodHandle.speciesData_LLLLL();
        names[THIS_MH]          = names[THIS_MH].withConstraint(data);
        names[GET_TARGET]       = new Name(data.getterFunction(0), names[THIS_MH]);
        names[GET_CLASS]        = new Name(data.getterFunction(1), names[THIS_MH]);
        names[GET_CATCHER]      = new Name(data.getterFunction(2), names[THIS_MH]);
        names[GET_COLLECT_ARGS] = new Name(data.getterFunction(3), names[THIS_MH]);
        names[GET_UNBOX_RESULT] = new Name(data.getterFunction(4), names[THIS_MH]);

        // FIXME: rework argument boxing/result unboxing logic for LF interpretation

        // t_{i}:L=MethodHandle.invokeBasic(collectArgs:L,a1:L,...);
        MethodType collectArgsType = basicType.changeReturnType(Object.class);
        MethodHandle invokeBasic = MethodHandles.basicInvoker(collectArgsType);
        Object[] args = new Object[invokeBasic.type().parameterCount()];
        args[0] = names[GET_COLLECT_ARGS];
        System.arraycopy(names, ARG_BASE, args, 1, ARG_LIMIT-ARG_BASE);
        names[BOXED_ARGS] = new Name(makeIntrinsic(invokeBasic, Intrinsic.GUARD_WITH_CATCH), args);

        // t_{i+1}:L=MethodHandleImpl.guardWithCatch(target:L,exType:L,catcher:L,t_{i}:L);
        Object[] gwcArgs = new Object[] {names[GET_TARGET], names[GET_CLASS], names[GET_CATCHER], names[BOXED_ARGS]};
        names[TRY_CATCH] = new Name(NF_guardWithCatch, gwcArgs);

        // t_{i+2}:I=MethodHandle.invokeBasic(unbox:L,t_{i+1}:L);
        MethodHandle invokeBasicUnbox = MethodHandles.basicInvoker(MethodType.methodType(basicType.rtype(), Object.class));
        Object[] unboxArgs  = new Object[] {names[GET_UNBOX_RESULT], names[TRY_CATCH]};
        names[UNBOX_RESULT] = new Name(invokeBasicUnbox, unboxArgs);

        lform = new LambdaForm("guardWithCatch", lambdaType.parameterCount(), names);

        return basicType.form().setCachedLambdaForm(MethodTypeForm.LF_GWC, lform);
    }

    static
    MethodHandle makeGuardWithCatch(MethodHandle target,
                                    Class<? extends Throwable> exType,
                                    MethodHandle catcher) {
        MethodType type = target.type();
        LambdaForm form = makeGuardWithCatchForm(type.basicType());

        // Prepare auxiliary method handles used during LambdaForm interpretation.
        // Box arguments and wrap them into Object[]: ValueConversions.array().
        MethodType varargsType = type.changeReturnType(Object[].class);
        MethodHandle collectArgs = varargsArray(type.parameterCount()).asType(varargsType);
        MethodHandle unboxResult = unboxResultHandle(type.returnType());

        BoundMethodHandle.SpeciesData data = BoundMethodHandle.speciesData_LLLLL();
        BoundMethodHandle mh;
        try {
            mh = (BoundMethodHandle) data.constructor().invokeBasic(type, form, (Object) target, (Object) exType,
                    (Object) catcher, (Object) collectArgs, (Object) unboxResult);
        } catch (Throwable ex) {
            throw uncaughtException(ex);
        }
        assert(mh.type() == type);
        return mh;
    }

    /**
     * Intrinsified during LambdaForm compilation
     * (see {@link InvokerBytecodeGenerator#emitGuardWithCatch emitGuardWithCatch}).
     */
    @LambdaForm.Hidden
    static Object guardWithCatch(MethodHandle target, Class<? extends Throwable> exType, MethodHandle catcher,
                                 Object... av) throws Throwable {
        // Use asFixedArity() to avoid unnecessary boxing of last argument for VarargsCollector case.
        try {
            return target.asFixedArity().invokeWithArguments(av);
        } catch (Throwable t) {
            if (!exType.isInstance(t)) throw t;
            return catcher.asFixedArity().invokeWithArguments(prepend(av, t));
        }
    }

    /** Prepend elements to an array. */
    @LambdaForm.Hidden
    private static Object[] prepend(Object[] array, Object... elems) {
        int nArray = array.length;
        int nElems = elems.length;
        Object[] newArray = new Object[nArray + nElems];
        System.arraycopy(elems, 0, newArray, 0, nElems);
        System.arraycopy(array, 0, newArray, nElems, nArray);
        return newArray;
    }

    static
    MethodHandle throwException(MethodType type) {
        assert(Throwable.class.isAssignableFrom(type.parameterType(0)));
        int arity = type.parameterCount();
        if (arity > 1) {
            MethodHandle mh = throwException(type.dropParameterTypes(1, arity));
            mh = MethodHandles.dropArguments(mh, 1, Arrays.copyOfRange(type.parameterArray(), 1, arity));
            return mh;
        }
        return makePairwiseConvert(NF_throwException.resolvedHandle(), type, false, true);
    }

    static <T extends Throwable> Empty throwException(T t) throws T { throw t; }

    static MethodHandle[] FAKE_METHOD_HANDLE_INVOKE = new MethodHandle[2];
    static MethodHandle fakeMethodHandleInvoke(MemberName method) {
        int idx;
        assert(method.isMethodHandleInvoke());
        switch (method.getName()) {
        case "invoke":       idx = 0; break;
        case "invokeExact":  idx = 1; break;
        default:             throw new InternalError(method.getName());
        }
        MethodHandle mh = FAKE_METHOD_HANDLE_INVOKE[idx];
        if (mh != null)  return mh;
        MethodType type = MethodType.methodType(Object.class, UnsupportedOperationException.class,
                                                MethodHandle.class, Object[].class);
        mh = throwException(type);
        mh = mh.bindTo(new UnsupportedOperationException("cannot reflectively invoke MethodHandle"));
        if (!method.getInvocationType().equals(mh.type()))
            throw new InternalError(method.toString());
        mh = mh.withInternalMemberName(method, false);
        mh = mh.withVarargs(true);
        assert(method.isVarargs());
        FAKE_METHOD_HANDLE_INVOKE[idx] = mh;
        return mh;
    }
    static MethodHandle fakeVarHandleInvoke(MemberName method) {
        // TODO caching, is it necessary?
        MethodType type = MethodType.methodType(method.getReturnType(), UnsupportedOperationException.class,
                                                VarHandle.class, Object[].class);
        MethodHandle mh = throwException(type);
        mh = mh.bindTo(new UnsupportedOperationException("cannot reflectively invoke VarHandle"));
        if (!method.getInvocationType().equals(mh.type()))
            throw new InternalError(method.toString());
        mh = mh.withInternalMemberName(method, false);
        mh = mh.asVarargsCollector(Object[].class);
        assert(method.isVarargs());
        return mh;
    }

    /**
     * Create an alias for the method handle which, when called,
     * appears to be called from the same class loader and protection domain
     * as hostClass.
     * This is an expensive no-op unless the method which is called
     * is sensitive to its caller.  A small number of system methods
     * are in this category, including Class.forName and Method.invoke.
     */
    static
    MethodHandle bindCaller(MethodHandle mh, Class<?> hostClass) {
        return BindCaller.bindCaller(mh, hostClass);
    }

    // Put the whole mess into its own nested class.
    // That way we can lazily load the code and set up the constants.
    private static class BindCaller {
        private static MethodType INVOKER_MT = MethodType.methodType(Object.class, MethodHandle.class, Object[].class);

        static
        MethodHandle bindCaller(MethodHandle mh, Class<?> hostClass) {
            // Do not use this function to inject calls into system classes.
            if (hostClass == null
                ||    (hostClass.isArray() ||
                       hostClass.isPrimitive() ||
                       hostClass.getName().startsWith("java.") ||
                       hostClass.getName().startsWith("sun."))) {
                throw new InternalError();  // does not happen, and should not anyway
            }
            // For simplicity, convert mh to a varargs-like method.
            MethodHandle vamh = prepareForInvoker(mh);
            // Cache the result of makeInjectedInvoker once per argument class.
            MethodHandle bccInvoker = CV_makeInjectedInvoker.get(hostClass);
            return restoreToType(bccInvoker.bindTo(vamh), mh, hostClass);
        }

        private static MethodHandle makeInjectedInvoker(Class<?> hostClass) {
            try {
                Class<?> invokerClass = UNSAFE.defineAnonymousClass(hostClass, INJECTED_INVOKER_TEMPLATE, null);
                assert checkInjectedInvoker(hostClass, invokerClass);
                return IMPL_LOOKUP.findStatic(invokerClass, "invoke_V", INVOKER_MT);
            } catch (ReflectiveOperationException ex) {
                throw uncaughtException(ex);
            }
        }

        private static ClassValue<MethodHandle> CV_makeInjectedInvoker = new ClassValue<MethodHandle>() {
            @Override protected MethodHandle computeValue(Class<?> hostClass) {
                return makeInjectedInvoker(hostClass);
            }
        };

        // Adapt mh so that it can be called directly from an injected invoker:
        private static MethodHandle prepareForInvoker(MethodHandle mh) {
            mh = mh.asFixedArity();
            MethodType mt = mh.type();
            int arity = mt.parameterCount();
            MethodHandle vamh = mh.asType(mt.generic());
            vamh.internalForm().compileToBytecode();  // eliminate LFI stack frames
            vamh = vamh.asSpreader(Object[].class, arity);
            vamh.internalForm().compileToBytecode();  // eliminate LFI stack frames
            return vamh;
        }

        // Undo the adapter effect of prepareForInvoker:
        private static MethodHandle restoreToType(MethodHandle vamh,
                                                  MethodHandle original,
                                                  Class<?> hostClass) {
            MethodType type = original.type();
            MethodHandle mh = vamh.asCollector(Object[].class, type.parameterCount());
            MemberName member = original.internalMemberName();
            mh = mh.asType(type);
            mh = new WrappedMember(mh, type, member, original.isInvokeSpecial(), hostClass);
            return mh;
        }

        private static boolean checkInjectedInvoker(Class<?> hostClass, Class<?> invokerClass) {
            assert (hostClass.getClassLoader() == invokerClass.getClassLoader()) : hostClass.getName()+" (CL)";
            try {
                assert (hostClass.getProtectionDomain() == invokerClass.getProtectionDomain()) : hostClass.getName()+" (PD)";
            } catch (SecurityException ex) {
                // Self-check was blocked by security manager. This is OK.
            }
            try {
                // Test the invoker to ensure that it really injects into the right place.
                MethodHandle invoker = IMPL_LOOKUP.findStatic(invokerClass, "invoke_V", INVOKER_MT);
                MethodHandle vamh = prepareForInvoker(MH_checkCallerClass);
                return (boolean)invoker.invoke(vamh, new Object[]{ invokerClass });
            } catch (Throwable ex) {
                throw new InternalError(ex);
            }
        }

        private static final MethodHandle MH_checkCallerClass;
        static {
            final Class<?> THIS_CLASS = BindCaller.class;
            assert(checkCallerClass(THIS_CLASS));
            try {
                MH_checkCallerClass = IMPL_LOOKUP
                    .findStatic(THIS_CLASS, "checkCallerClass",
                                MethodType.methodType(boolean.class, Class.class));
                assert((boolean) MH_checkCallerClass.invokeExact(THIS_CLASS));
            } catch (Throwable ex) {
                throw new InternalError(ex);
            }
        }

        @CallerSensitive
        @ForceInline // to ensure Reflection.getCallerClass optimization
        private static boolean checkCallerClass(Class<?> expected) {
            // This method is called via MH_checkCallerClass and so it's correct to ask for the immediate caller here.
            Class<?> actual = Reflection.getCallerClass();
            if (actual != expected)
                throw new InternalError("found " + actual.getName() + ", expected " + expected.getName());
            return true;
        }

        private static final byte[] INJECTED_INVOKER_TEMPLATE = generateInvokerTemplate();

        /** Produces byte code for a class that is used as an injected invoker. */
        private static byte[] generateInvokerTemplate() {
            ClassWriter cw = new ClassWriter(0);

            // private static class InjectedInvoker {
            //     @Hidden
            //     static Object invoke_V(MethodHandle vamh, Object[] args) throws Throwable {
            //        return vamh.invokeExact(args);
            //     }
            // }
            cw.visit(52, ACC_PRIVATE | ACC_SUPER, "InjectedInvoker", null, "java/lang/Object", null);

            MethodVisitor mv = cw.visitMethod(ACC_STATIC, "invoke_V",
                          "(Ljava/lang/invoke/MethodHandle;[Ljava/lang/Object;)Ljava/lang/Object;",
                          null, null);

            // Suppress invoker method in stack traces.
            AnnotationVisitor av0 = mv.visitAnnotation("Ljava/lang/invoke/LambdaForm$Hidden;", true);
            av0.visitEnd();

            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact",
                               "([Ljava/lang/Object;)Ljava/lang/Object;", false);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();

            cw.visitEnd();
            return cw.toByteArray();
        }
    }

    /** This subclass allows a wrapped method handle to be re-associated with an arbitrary member name. */
    private static final class WrappedMember extends DelegatingMethodHandle {
        private final MethodHandle target;
        private final MemberName member;
        private final Class<?> callerClass;
        private final boolean isInvokeSpecial;

        private WrappedMember(MethodHandle target, MethodType type,
                              MemberName member, boolean isInvokeSpecial,
                              Class<?> callerClass) {
            super(type, target);
            this.target = target;
            this.member = member;
            this.callerClass = callerClass;
            this.isInvokeSpecial = isInvokeSpecial;
        }

        @Override
        MemberName internalMemberName() {
            return member;
        }
        @Override
        Class<?> internalCallerClass() {
            return callerClass;
        }
        @Override
        boolean isInvokeSpecial() {
            return isInvokeSpecial;
        }
        @Override
        protected MethodHandle getTarget() {
            return target;
        }
        @Override
        public MethodHandle asTypeUncached(MethodType newType) {
            // This MH is an alias for target, except for the MemberName
            // Drop the MemberName if there is any conversion.
            return asTypeCache = target.asType(newType);
        }
    }

    static MethodHandle makeWrappedMember(MethodHandle target, MemberName member, boolean isInvokeSpecial) {
        if (member.equals(target.internalMemberName()) && isInvokeSpecial == target.isInvokeSpecial())
            return target;
        return new WrappedMember(target, target.type(), member, isInvokeSpecial, null);
    }

    /** Intrinsic IDs */
    /*non-public*/
    enum Intrinsic {
        SELECT_ALTERNATIVE,
        GUARD_WITH_CATCH,
        TRY_FINALLY,
        LOOP,
        NEW_ARRAY,
        ARRAY_LOAD,
        ARRAY_STORE,
        ARRAY_LENGTH,
        IDENTITY,
        ZERO,
        NONE // no intrinsic associated
    }

    /** Mark arbitrary method handle as intrinsic.
     * InvokerBytecodeGenerator uses this info to produce more efficient bytecode shape. */
    static final class IntrinsicMethodHandle extends DelegatingMethodHandle {
        private final MethodHandle target;
        private final Intrinsic intrinsicName;

        IntrinsicMethodHandle(MethodHandle target, Intrinsic intrinsicName) {
            super(target.type(), target);
            this.target = target;
            this.intrinsicName = intrinsicName;
        }

        @Override
        protected MethodHandle getTarget() {
            return target;
        }

        @Override
        Intrinsic intrinsicName() {
            return intrinsicName;
        }

        @Override
        public MethodHandle asTypeUncached(MethodType newType) {
            // This MH is an alias for target, except for the intrinsic name
            // Drop the name if there is any conversion.
            return asTypeCache = target.asType(newType);
        }

        @Override
        String internalProperties() {
            return super.internalProperties() +
                    "\n& Intrinsic="+intrinsicName;
        }

        @Override
        public MethodHandle asCollector(Class<?> arrayType, int arrayLength) {
            if (intrinsicName == Intrinsic.IDENTITY) {
                MethodType resultType = type().asCollectorType(arrayType, type().parameterCount() - 1, arrayLength);
                MethodHandle newArray = MethodHandleImpl.varargsArray(arrayType, arrayLength);
                return newArray.asType(resultType);
            }
            return super.asCollector(arrayType, arrayLength);
        }
    }

    static MethodHandle makeIntrinsic(MethodHandle target, Intrinsic intrinsicName) {
        if (intrinsicName == target.intrinsicName())
            return target;
        return new IntrinsicMethodHandle(target, intrinsicName);
    }

    static MethodHandle makeIntrinsic(MethodType type, LambdaForm form, Intrinsic intrinsicName) {
        return new IntrinsicMethodHandle(SimpleMethodHandle.make(type, form), intrinsicName);
    }

    /// Collection of multiple arguments.

    private static MethodHandle findCollector(String name, int nargs, Class<?> rtype, Class<?>... ptypes) {
        MethodType type = MethodType.genericMethodType(nargs)
                .changeReturnType(rtype)
                .insertParameterTypes(0, ptypes);
        try {
            return IMPL_LOOKUP.findStatic(MethodHandleImpl.class, name, type);
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    private static final Object[] NO_ARGS_ARRAY = {};
    private static Object[] makeArray(Object... args) { return args; }
    private static Object[] array() { return NO_ARGS_ARRAY; }
    private static Object[] array(Object a0)
                { return makeArray(a0); }
    private static Object[] array(Object a0, Object a1)
                { return makeArray(a0, a1); }
    private static Object[] array(Object a0, Object a1, Object a2)
                { return makeArray(a0, a1, a2); }
    private static Object[] array(Object a0, Object a1, Object a2, Object a3)
                { return makeArray(a0, a1, a2, a3); }
    private static Object[] array(Object a0, Object a1, Object a2, Object a3,
                                  Object a4)
                { return makeArray(a0, a1, a2, a3, a4); }
    private static Object[] array(Object a0, Object a1, Object a2, Object a3,
                                  Object a4, Object a5)
                { return makeArray(a0, a1, a2, a3, a4, a5); }
    private static Object[] array(Object a0, Object a1, Object a2, Object a3,
                                  Object a4, Object a5, Object a6)
                { return makeArray(a0, a1, a2, a3, a4, a5, a6); }
    private static Object[] array(Object a0, Object a1, Object a2, Object a3,
                                  Object a4, Object a5, Object a6, Object a7)
                { return makeArray(a0, a1, a2, a3, a4, a5, a6, a7); }
    private static Object[] array(Object a0, Object a1, Object a2, Object a3,
                                  Object a4, Object a5, Object a6, Object a7,
                                  Object a8)
                { return makeArray(a0, a1, a2, a3, a4, a5, a6, a7, a8); }
    private static Object[] array(Object a0, Object a1, Object a2, Object a3,
                                  Object a4, Object a5, Object a6, Object a7,
                                  Object a8, Object a9)
                { return makeArray(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9); }

    private static final int ARRAYS_COUNT = 11;
    private static final @Stable MethodHandle[] ARRAYS = new MethodHandle[MAX_ARITY + 1];

    // filling versions of the above:
    // using Integer len instead of int len and no varargs to avoid bootstrapping problems
    private static Object[] fillNewArray(Integer len, Object[] /*not ...*/ args) {
        Object[] a = new Object[len];
        fillWithArguments(a, 0, args);
        return a;
    }
    private static Object[] fillNewTypedArray(Object[] example, Integer len, Object[] /*not ...*/ args) {
        Object[] a = Arrays.copyOf(example, len);
        assert(a.getClass() != Object[].class);
        fillWithArguments(a, 0, args);
        return a;
    }
    private static void fillWithArguments(Object[] a, int pos, Object... args) {
        System.arraycopy(args, 0, a, pos, args.length);
    }
    // using Integer pos instead of int pos to avoid bootstrapping problems
    private static Object[] fillArray(Integer pos, Object[] a, Object a0)
                { fillWithArguments(a, pos, a0); return a; }
    private static Object[] fillArray(Integer pos, Object[] a, Object a0, Object a1)
                { fillWithArguments(a, pos, a0, a1); return a; }
    private static Object[] fillArray(Integer pos, Object[] a, Object a0, Object a1, Object a2)
                { fillWithArguments(a, pos, a0, a1, a2); return a; }
    private static Object[] fillArray(Integer pos, Object[] a, Object a0, Object a1, Object a2, Object a3)
                { fillWithArguments(a, pos, a0, a1, a2, a3); return a; }
    private static Object[] fillArray(Integer pos, Object[] a, Object a0, Object a1, Object a2, Object a3,
                                  Object a4)
                { fillWithArguments(a, pos, a0, a1, a2, a3, a4); return a; }
    private static Object[] fillArray(Integer pos, Object[] a, Object a0, Object a1, Object a2, Object a3,
                                  Object a4, Object a5)
                { fillWithArguments(a, pos, a0, a1, a2, a3, a4, a5); return a; }
    private static Object[] fillArray(Integer pos, Object[] a, Object a0, Object a1, Object a2, Object a3,
                                  Object a4, Object a5, Object a6)
                { fillWithArguments(a, pos, a0, a1, a2, a3, a4, a5, a6); return a; }
    private static Object[] fillArray(Integer pos, Object[] a, Object a0, Object a1, Object a2, Object a3,
                                  Object a4, Object a5, Object a6, Object a7)
                { fillWithArguments(a, pos, a0, a1, a2, a3, a4, a5, a6, a7); return a; }
    private static Object[] fillArray(Integer pos, Object[] a, Object a0, Object a1, Object a2, Object a3,
                                  Object a4, Object a5, Object a6, Object a7,
                                  Object a8)
                { fillWithArguments(a, pos, a0, a1, a2, a3, a4, a5, a6, a7, a8); return a; }
    private static Object[] fillArray(Integer pos, Object[] a, Object a0, Object a1, Object a2, Object a3,
                                  Object a4, Object a5, Object a6, Object a7,
                                  Object a8, Object a9)
                { fillWithArguments(a, pos, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9); return a; }

    private static final int FILL_ARRAYS_COUNT = 11; // current number of fillArray methods
    private static final @Stable MethodHandle[] FILL_ARRAYS = new MethodHandle[FILL_ARRAYS_COUNT];

    private static MethodHandle getFillArray(int count) {
        assert (count > 0 && count < FILL_ARRAYS_COUNT);
        MethodHandle mh = FILL_ARRAYS[count];
        if (mh != null) {
            return mh;
        }
        mh = findCollector("fillArray", count, Object[].class, Integer.class, Object[].class);
        FILL_ARRAYS[count] = mh;
        return mh;
    }

    private static Object copyAsPrimitiveArray(Wrapper w, Object... boxes) {
        Object a = w.makeArray(boxes.length);
        w.copyArrayUnboxing(boxes, 0, a, 0, boxes.length);
        return a;
    }

    /** Return a method handle that takes the indicated number of Object
     *  arguments and returns an Object array of them, as if for varargs.
     */
    static MethodHandle varargsArray(int nargs) {
        MethodHandle mh = ARRAYS[nargs];
        if (mh != null) {
            return mh;
        }
        if (nargs < ARRAYS_COUNT) {
            mh = findCollector("array", nargs, Object[].class);
        } else {
            mh = buildVarargsArray(getConstantHandle(MH_fillNewArray),
                    getConstantHandle(MH_arrayIdentity), nargs);
        }
        assert(assertCorrectArity(mh, nargs));
        mh = makeIntrinsic(mh, Intrinsic.NEW_ARRAY);
        return ARRAYS[nargs] = mh;
    }

    private static boolean assertCorrectArity(MethodHandle mh, int arity) {
        assert(mh.type().parameterCount() == arity) : "arity != "+arity+": "+mh;
        return true;
    }

    // Array identity function (used as getConstantHandle(MH_arrayIdentity)).
    static <T> T[] identity(T[] x) {
        return x;
    }

    private static MethodHandle buildVarargsArray(MethodHandle newArray, MethodHandle finisher, int nargs) {
        // Build up the result mh as a sequence of fills like this:
        //   finisher(fill(fill(newArrayWA(23,x1..x10),10,x11..x20),20,x21..x23))
        // The various fill(_,10*I,___*[J]) are reusable.
        int leftLen = Math.min(nargs, LEFT_ARGS);  // absorb some arguments immediately
        int rightLen = nargs - leftLen;
        MethodHandle leftCollector = newArray.bindTo(nargs);
        leftCollector = leftCollector.asCollector(Object[].class, leftLen);
        MethodHandle mh = finisher;
        if (rightLen > 0) {
            MethodHandle rightFiller = fillToRight(LEFT_ARGS + rightLen);
            if (mh.equals(getConstantHandle(MH_arrayIdentity)))
                mh = rightFiller;
            else
                mh = MethodHandles.collectArguments(mh, 0, rightFiller);
        }
        if (mh.equals(getConstantHandle(MH_arrayIdentity)))
            mh = leftCollector;
        else
            mh = MethodHandles.collectArguments(mh, 0, leftCollector);
        return mh;
    }

    private static final int LEFT_ARGS = FILL_ARRAYS_COUNT - 1;
    private static final @Stable MethodHandle[] FILL_ARRAY_TO_RIGHT = new MethodHandle[MAX_ARITY + 1];
    /** fill_array_to_right(N).invoke(a, argL..arg[N-1])
     *  fills a[L]..a[N-1] with corresponding arguments,
     *  and then returns a.  The value L is a global constant (LEFT_ARGS).
     */
    private static MethodHandle fillToRight(int nargs) {
        MethodHandle filler = FILL_ARRAY_TO_RIGHT[nargs];
        if (filler != null)  return filler;
        filler = buildFiller(nargs);
        assert(assertCorrectArity(filler, nargs - LEFT_ARGS + 1));
        return FILL_ARRAY_TO_RIGHT[nargs] = filler;
    }
    private static MethodHandle buildFiller(int nargs) {
        if (nargs <= LEFT_ARGS)
            return getConstantHandle(MH_arrayIdentity);  // no args to fill; return the array unchanged
        // we need room for both mh and a in mh.invoke(a, arg*[nargs])
        final int CHUNK = LEFT_ARGS;
        int rightLen = nargs % CHUNK;
        int midLen = nargs - rightLen;
        if (rightLen == 0) {
            midLen = nargs - (rightLen = CHUNK);
            if (FILL_ARRAY_TO_RIGHT[midLen] == null) {
                // build some precursors from left to right
                for (int j = LEFT_ARGS % CHUNK; j < midLen; j += CHUNK)
                    if (j > LEFT_ARGS)  fillToRight(j);
            }
        }
        if (midLen < LEFT_ARGS) rightLen = nargs - (midLen = LEFT_ARGS);
        assert(rightLen > 0);
        MethodHandle midFill = fillToRight(midLen);  // recursive fill
        MethodHandle rightFill = getFillArray(rightLen).bindTo(midLen);  // [midLen..nargs-1]
        assert(midFill.type().parameterCount()   == 1 + midLen - LEFT_ARGS);
        assert(rightFill.type().parameterCount() == 1 + rightLen);

        // Combine the two fills:
        //   right(mid(a, x10..x19), x20..x23)
        // The final product will look like this:
        //   right(mid(newArrayLeft(24, x0..x9), x10..x19), x20..x23)
        if (midLen == LEFT_ARGS)
            return rightFill;
        else
            return MethodHandles.collectArguments(rightFill, 0, midFill);
    }

    static final int MAX_JVM_ARITY = 255;  // limit imposed by the JVM

    /** Return a method handle that takes the indicated number of
     *  typed arguments and returns an array of them.
     *  The type argument is the array type.
     */
    static MethodHandle varargsArray(Class<?> arrayType, int nargs) {
        Class<?> elemType = arrayType.getComponentType();
        if (elemType == null)  throw new IllegalArgumentException("not an array: "+arrayType);
        // FIXME: Need more special casing and caching here.
        if (nargs >= MAX_JVM_ARITY/2 - 1) {
            int slots = nargs;
            final int MAX_ARRAY_SLOTS = MAX_JVM_ARITY - 1;  // 1 for receiver MH
            if (slots <= MAX_ARRAY_SLOTS && elemType.isPrimitive())
                slots *= Wrapper.forPrimitiveType(elemType).stackSlots();
            if (slots > MAX_ARRAY_SLOTS)
                throw new IllegalArgumentException("too many arguments: "+arrayType.getSimpleName()+", length "+nargs);
        }
        if (elemType == Object.class)
            return varargsArray(nargs);
        // other cases:  primitive arrays, subtypes of Object[]
        MethodHandle cache[] = Makers.TYPED_COLLECTORS.get(elemType);
        MethodHandle mh = nargs < cache.length ? cache[nargs] : null;
        if (mh != null)  return mh;
        if (nargs == 0) {
            Object example = java.lang.reflect.Array.newInstance(arrayType.getComponentType(), 0);
            mh = MethodHandles.constant(arrayType, example);
        } else if (elemType.isPrimitive()) {
            MethodHandle builder = getConstantHandle(MH_fillNewArray);
            MethodHandle producer = buildArrayProducer(arrayType);
            mh = buildVarargsArray(builder, producer, nargs);
        } else {
            Class<? extends Object[]> objArrayType = arrayType.asSubclass(Object[].class);
            Object[] example = Arrays.copyOf(NO_ARGS_ARRAY, 0, objArrayType);
            MethodHandle builder = getConstantHandle(MH_fillNewTypedArray).bindTo(example);
            MethodHandle producer = getConstantHandle(MH_arrayIdentity); // must be weakly typed
            mh = buildVarargsArray(builder, producer, nargs);
        }
        mh = mh.asType(MethodType.methodType(arrayType, Collections.<Class<?>>nCopies(nargs, elemType)));
        mh = makeIntrinsic(mh, Intrinsic.NEW_ARRAY);
        assert(assertCorrectArity(mh, nargs));
        if (nargs < cache.length)
            cache[nargs] = mh;
        return mh;
    }

    private static MethodHandle buildArrayProducer(Class<?> arrayType) {
        Class<?> elemType = arrayType.getComponentType();
        assert(elemType.isPrimitive());
        return getConstantHandle(MH_copyAsPrimitiveArray).bindTo(Wrapper.forPrimitiveType(elemType));
    }

    /*non-public*/ static void assertSame(Object mh1, Object mh2) {
        if (mh1 != mh2) {
            String msg = String.format("mh1 != mh2: mh1 = %s (form: %s); mh2 = %s (form: %s)",
                    mh1, ((MethodHandle)mh1).form,
                    mh2, ((MethodHandle)mh2).form);
            throw newInternalError(msg);
        }
    }

    // Local constant functions:
    /*non-public*/ static final NamedFunction
        NF_checkSpreadArgument,
        NF_guardWithCatch,
        NF_throwException,
        NF_tryFinally,
        NF_loop,
        NF_profileBoolean;

    static {
        try {
            NF_checkSpreadArgument = new NamedFunction(MethodHandleImpl.class
                    .getDeclaredMethod("checkSpreadArgument", Object.class, int.class));
            NF_guardWithCatch = new NamedFunction(MethodHandleImpl.class
                    .getDeclaredMethod("guardWithCatch", MethodHandle.class, Class.class,
                            MethodHandle.class, Object[].class));
            NF_tryFinally = new NamedFunction(MethodHandleImpl.class
                    .getDeclaredMethod("tryFinally", MethodHandle.class, MethodHandle.class, Object[].class));
            NF_loop = new NamedFunction(MethodHandleImpl.class
                    .getDeclaredMethod("loop", BasicType[].class, MethodHandle[].class, MethodHandle[].class,
                            MethodHandle[].class, MethodHandle[].class, Object[].class));
            NF_throwException = new NamedFunction(MethodHandleImpl.class
                    .getDeclaredMethod("throwException", Throwable.class));
            NF_profileBoolean = new NamedFunction(MethodHandleImpl.class
                    .getDeclaredMethod("profileBoolean", boolean.class, int[].class));
        } catch (ReflectiveOperationException ex) {
            throw newInternalError(ex);
        }

        SharedSecrets.setJavaLangInvokeAccess(new JavaLangInvokeAccess() {
            @Override
            public Object newMemberName() {
                return new MemberName();
            }

            @Override
            public String getName(Object mname) {
                MemberName memberName = (MemberName)mname;
                return memberName.getName();
            }

            @Override
            public boolean isNative(Object mname) {
                MemberName memberName = (MemberName)mname;
                return memberName.isNative();
            }

            @Override
            public byte[] generateDirectMethodHandleHolderClassBytes(
                    String className, MethodType[] methodTypes, int[] types) {
                return GenerateJLIClassesHelper
                        .generateDirectMethodHandleHolderClassBytes(
                                className, methodTypes, types);
            }

            @Override
            public byte[] generateDelegatingMethodHandleHolderClassBytes(
                    String className, MethodType[] methodTypes) {
                return GenerateJLIClassesHelper
                        .generateDelegatingMethodHandleHolderClassBytes(
                                className, methodTypes);
            }

            @Override
            public Map.Entry<String, byte[]> generateConcreteBMHClassBytes(
                    final String types) {
                return GenerateJLIClassesHelper
                        .generateConcreteBMHClassBytes(types);
            }
        });
    }

    /** Result unboxing: ValueConversions.unbox() OR ValueConversions.identity() OR ValueConversions.ignore(). */
    private static MethodHandle unboxResultHandle(Class<?> returnType) {
        if (returnType.isPrimitive()) {
            if (returnType == void.class) {
                return ValueConversions.ignore();
            } else {
                Wrapper w = Wrapper.forPrimitiveType(returnType);
                return ValueConversions.unboxExact(w);
            }
        } else {
            return MethodHandles.identity(Object.class);
        }
    }

    /**
     * Assembles a loop method handle from the given handles and type information.
     *
     * @param tloop the return type of the loop.
     * @param targs types of the arguments to be passed to the loop.
     * @param init sanitized array of initializers for loop-local variables.
     * @param step sanitited array of loop bodies.
     * @param pred sanitized array of predicates.
     * @param fini sanitized array of loop finalizers.
     *
     * @return a handle that, when invoked, will execute the loop.
     */
    static MethodHandle makeLoop(Class<?> tloop, List<Class<?>> targs, List<MethodHandle> init, List<MethodHandle> step,
                                 List<MethodHandle> pred, List<MethodHandle> fini) {
        MethodType type = MethodType.methodType(tloop, targs);
        BasicType[] initClauseTypes =
                init.stream().map(h -> h.type().returnType()).map(BasicType::basicType).toArray(BasicType[]::new);
        LambdaForm form = makeLoopForm(type.basicType(), initClauseTypes);

        // Prepare auxiliary method handles used during LambdaForm interpretation.
        // Box arguments and wrap them into Object[]: ValueConversions.array().
        MethodType varargsType = type.changeReturnType(Object[].class);
        MethodHandle collectArgs = varargsArray(type.parameterCount()).asType(varargsType);
        MethodHandle unboxResult = unboxResultHandle(tloop);

        BoundMethodHandle.SpeciesData data = BoundMethodHandle.speciesData_LLLLLL();
        BoundMethodHandle mh;
        try {
            mh = (BoundMethodHandle) data.constructor().invokeBasic(type, form, (Object) toArray(init),
                    (Object) toArray(step), (Object) toArray(pred), (Object) toArray(fini), (Object) collectArgs,
                    (Object) unboxResult);
        } catch (Throwable ex) {
            throw uncaughtException(ex);
        }
        assert(mh.type() == type);
        return mh;
    }

    private static MethodHandle[] toArray(List<MethodHandle> l) {
        return l.toArray(new MethodHandle[0]);
    }

    /**
     * Loops introduce some complexity as they can have additional local state. Hence, LambdaForms for loops are
     * generated from a template. The LambdaForm template shape for the loop combinator is as follows (assuming one
     * reference parameter passed in {@code a1}, and a reference return type, with the return value represented by
     * {@code t12}):
     * <blockquote><pre>{@code
     *  loop=Lambda(a0:L,a1:L)=>{
     *    t2:L=BoundMethodHandle$Species_L6.argL0(a0:L);             // array of init method handles
     *    t3:L=BoundMethodHandle$Species_L6.argL1(a0:L);             // array of step method handles
     *    t4:L=BoundMethodHandle$Species_L6.argL2(a0:L);             // array of pred method handles
     *    t5:L=BoundMethodHandle$Species_L6.argL3(a0:L);             // array of fini method handles
     *    t6:L=BoundMethodHandle$Species_L6.argL4(a0:L);             // helper handle to box the arguments into an Object[]
     *    t7:L=BoundMethodHandle$Species_L6.argL5(a0:L);             // helper handle to unbox the result
     *    t8:L=MethodHandle.invokeBasic(t6:L,a1:L);                  // box the arguments into an Object[]
     *    t9:L=MethodHandleImpl.loop(null,t2:L,t3:L,t4:L,t5:L,t6:L); // call the loop executor
     *    t10:L=MethodHandle.invokeBasic(t7:L,t9:L);t10:L}           // unbox the result; return the result
     * }</pre></blockquote>
     * <p>
     * {@code argL0} through {@code argL3} are the arrays of init, step, pred, and fini method handles.
     * {@code argL4} and {@code argL5} are auxiliary method handles: {@code argL2} boxes arguments and wraps them into
     * {@code Object[]} ({@code ValueConversions.array()}), and {@code argL3} unboxes the result if necessary
     * ({@code ValueConversions.unbox()}).
     * <p>
     * Having {@code t6} and {@code t7} passed in via a BMH and not hardcoded in the lambda form allows to share lambda
     * forms among loop combinators with the same basic type.
     * <p>
     * The above template is instantiated by using the {@link LambdaFormEditor} to replace the {@code null} argument to
     * the {@code loop} invocation with the {@code BasicType} array describing the loop clause types. This argument is
     * ignored in the loop invoker, but will be extracted and used in {@linkplain InvokerBytecodeGenerator#emitLoop(int)
     * bytecode generation}.
     */
    private static LambdaForm makeLoopForm(MethodType basicType, BasicType[] localVarTypes) {
        MethodType lambdaType = basicType.invokerType();

        final int THIS_MH = 0;  // the BMH_LLLLLL
        final int ARG_BASE = 1; // start of incoming arguments
        final int ARG_LIMIT = ARG_BASE + basicType.parameterCount();

        int nameCursor = ARG_LIMIT;
        final int GET_INITS = nameCursor++;
        final int GET_STEPS = nameCursor++;
        final int GET_PREDS = nameCursor++;
        final int GET_FINIS = nameCursor++;
        final int GET_COLLECT_ARGS = nameCursor++;
        final int GET_UNBOX_RESULT = nameCursor++;
        final int BOXED_ARGS = nameCursor++;
        final int LOOP = nameCursor++;
        final int UNBOX_RESULT = nameCursor++;

        LambdaForm lform = basicType.form().cachedLambdaForm(MethodTypeForm.LF_LOOP);
        if (lform == null) {
            Name[] names = arguments(nameCursor - ARG_LIMIT, lambdaType);

            BoundMethodHandle.SpeciesData data = BoundMethodHandle.speciesData_LLLLLL();
            names[THIS_MH] = names[THIS_MH].withConstraint(data);
            names[GET_INITS] = new Name(data.getterFunction(0), names[THIS_MH]);
            names[GET_STEPS] = new Name(data.getterFunction(1), names[THIS_MH]);
            names[GET_PREDS] = new Name(data.getterFunction(2), names[THIS_MH]);
            names[GET_FINIS] = new Name(data.getterFunction(3), names[THIS_MH]);
            names[GET_COLLECT_ARGS] = new Name(data.getterFunction(4), names[THIS_MH]);
            names[GET_UNBOX_RESULT] = new Name(data.getterFunction(5), names[THIS_MH]);

            // t_{i}:L=MethodHandle.invokeBasic(collectArgs:L,a1:L,...);
            MethodType collectArgsType = basicType.changeReturnType(Object.class);
            MethodHandle invokeBasic = MethodHandles.basicInvoker(collectArgsType);
            Object[] args = new Object[invokeBasic.type().parameterCount()];
            args[0] = names[GET_COLLECT_ARGS];
            System.arraycopy(names, ARG_BASE, args, 1, ARG_LIMIT - ARG_BASE);
            names[BOXED_ARGS] = new Name(makeIntrinsic(invokeBasic, Intrinsic.LOOP), args);

            // t_{i+1}:L=MethodHandleImpl.loop(localTypes:L,inits:L,steps:L,preds:L,finis:L,t_{i}:L);
            Object[] lArgs =
                    new Object[]{null, // placeholder for BasicType[] localTypes - will be added by LambdaFormEditor
                            names[GET_INITS], names[GET_STEPS], names[GET_PREDS], names[GET_FINIS], names[BOXED_ARGS]};
            names[LOOP] = new Name(NF_loop, lArgs);

            // t_{i+2}:I=MethodHandle.invokeBasic(unbox:L,t_{i+1}:L);
            MethodHandle invokeBasicUnbox = MethodHandles.basicInvoker(MethodType.methodType(basicType.rtype(), Object.class));
            Object[] unboxArgs = new Object[]{names[GET_UNBOX_RESULT], names[LOOP]};
            names[UNBOX_RESULT] = new Name(invokeBasicUnbox, unboxArgs);

            lform = basicType.form().setCachedLambdaForm(MethodTypeForm.LF_LOOP,
                    new LambdaForm("loop", lambdaType.parameterCount(), names));
        }

        // BOXED_ARGS is the index into the names array where the loop idiom starts
        return lform.editor().noteLoopLocalTypesForm(BOXED_ARGS, localVarTypes);
    }


    /**
     * Intrinsified during LambdaForm compilation
     * (see {@link InvokerBytecodeGenerator#emitLoop(int)}).
     */
    @LambdaForm.Hidden
    static Object loop(BasicType[] localTypes, MethodHandle[] init, MethodHandle[] step, MethodHandle[] pred,
                       MethodHandle[] fini, Object... av) throws Throwable {
        int varSize = (int) Stream.of(init).filter(h -> h.type().returnType() != void.class).count();
        int nArgs = init[0].type().parameterCount();
        Object[] varsAndArgs = new Object[varSize + nArgs];
        for (int i = 0, v = 0; i < init.length; ++i) {
            if (init[i].type().returnType() == void.class) {
                init[i].asFixedArity().invokeWithArguments(av);
            } else {
                varsAndArgs[v++] = init[i].asFixedArity().invokeWithArguments(av);
            }
        }
        System.arraycopy(av, 0, varsAndArgs, varSize, nArgs);
        final int nSteps = step.length;
        for (; ; ) {
            for (int i = 0, v = 0; i < nSteps; ++i) {
                MethodHandle p = pred[i];
                MethodHandle s = step[i];
                MethodHandle f = fini[i];
                if (s.type().returnType() == void.class) {
                    s.asFixedArity().invokeWithArguments(varsAndArgs);
                } else {
                    varsAndArgs[v++] = s.asFixedArity().invokeWithArguments(varsAndArgs);
                }
                if (!(boolean) p.asFixedArity().invokeWithArguments(varsAndArgs)) {
                    return f.asFixedArity().invokeWithArguments(varsAndArgs);
                }
            }
        }
    }

    /**
     * This method is bound as the predicate in {@linkplain MethodHandles#countedLoop(MethodHandle, MethodHandle,
     * MethodHandle) counting loops}.
     *
     * @param counter the counter parameter, passed in during loop execution.
     * @param limit the upper bound of the parameter, statically bound at loop creation time.
     *
     * @return whether the counter has reached the limit.
     */
    static boolean countedLoopPredicate(int counter, int limit) {
        return counter <= limit;
    }

    /**
     * This method is bound as the step function in {@linkplain MethodHandles#countedLoop(MethodHandle, MethodHandle,
     * MethodHandle) counting loops} to increment the counter.
     *
     * @param counter the loop counter.
     *
     * @return the loop counter incremented by 1.
     */
    static int countedLoopStep(int counter, int limit) {
        return counter + 1;
    }

    /**
     * This method is bound as a filter in {@linkplain MethodHandles#countedLoop(MethodHandle, MethodHandle, MethodHandle,
     * MethodHandle) counting loops} to pass the correct counter value to the body.
     *
     * @param counter the loop counter.
     *
     * @return the loop counter decremented by 1.
     */
    static int decrementCounter(int counter) {
        return counter - 1;
    }

    /**
     * This is bound to initialize the loop-local iterator in {@linkplain MethodHandles#iteratedLoop iterating loops}.
     *
     * @param it the {@link Iterable} over which the loop iterates.
     *
     * @return an {@link Iterator} over the argument's elements.
     */
    static Iterator<?> initIterator(Iterable<?> it) {
        return it.iterator();
    }

    /**
     * This method is bound as the predicate in {@linkplain MethodHandles#iteratedLoop iterating loops}.
     *
     * @param it the iterator to be checked.
     *
     * @return {@code true} iff there are more elements to iterate over.
     */
    static boolean iteratePredicate(Iterator<?> it) {
        return it.hasNext();
    }

    /**
     * This method is bound as the step for retrieving the current value from the iterator in {@linkplain
     * MethodHandles#iteratedLoop iterating loops}.
     *
     * @param it the iterator.
     *
     * @return the next element from the iterator.
     */
    static Object iterateNext(Iterator<?> it) {
        return it.next();
    }

    /**
     * Makes a {@code try-finally} handle that conforms to the type constraints.
     *
     * @param target the target to execute in a {@code try-finally} block.
     * @param cleanup the cleanup to execute in the {@code finally} block.
     * @param rtype the result type of the entire construct.
     * @param argTypes the types of the arguments.
     *
     * @return a handle on the constructed {@code try-finally} block.
     */
    static MethodHandle makeTryFinally(MethodHandle target, MethodHandle cleanup, Class<?> rtype, List<Class<?>> argTypes) {
        MethodType type = MethodType.methodType(rtype, argTypes);
        LambdaForm form = makeTryFinallyForm(type.basicType());

        // Prepare auxiliary method handles used during LambdaForm interpretation.
        // Box arguments and wrap them into Object[]: ValueConversions.array().
        MethodType varargsType = type.changeReturnType(Object[].class);
        MethodHandle collectArgs = varargsArray(type.parameterCount()).asType(varargsType);
        MethodHandle unboxResult = unboxResultHandle(rtype);

        BoundMethodHandle.SpeciesData data = BoundMethodHandle.speciesData_LLLL();
        BoundMethodHandle mh;
        try {
            mh = (BoundMethodHandle) data.constructor().invokeBasic(type, form, (Object) target, (Object) cleanup,
                    (Object) collectArgs, (Object) unboxResult);
        } catch (Throwable ex) {
            throw uncaughtException(ex);
        }
        assert(mh.type() == type);
        return mh;
    }

    /**
     * The LambdaForm shape for the tryFinally combinator is as follows (assuming one reference parameter passed in
     * {@code a1}, and a reference return type, with the return value represented by {@code t8}):
     * <blockquote><pre>{@code
     *  tryFinally=Lambda(a0:L,a1:L)=>{
     *    t2:L=BoundMethodHandle$Species_LLLL.argL0(a0:L);  // target method handle
     *    t3:L=BoundMethodHandle$Species_LLLL.argL1(a0:L);  // cleanup method handle
     *    t4:L=BoundMethodHandle$Species_LLLL.argL2(a0:L);  // helper handle to box the arguments into an Object[]
     *    t5:L=BoundMethodHandle$Species_LLLL.argL3(a0:L);  // helper handle to unbox the result
     *    t6:L=MethodHandle.invokeBasic(t4:L,a1:L);         // box the arguments into an Object[]
     *    t7:L=MethodHandleImpl.tryFinally(t2:L,t3:L,t6:L); // call the tryFinally executor
     *    t8:L=MethodHandle.invokeBasic(t5:L,t7:L);t8:L}    // unbox the result; return the result
     * }</pre></blockquote>
     * <p>
     * {@code argL0} and {@code argL1} are the target and cleanup method handles.
     * {@code argL2} and {@code argL3} are auxiliary method handles: {@code argL2} boxes arguments and wraps them into
     * {@code Object[]} ({@code ValueConversions.array()}), and {@code argL3} unboxes the result if necessary
     * ({@code ValueConversions.unbox()}).
     * <p>
     * Having {@code t4} and {@code t5} passed in via a BMH and not hardcoded in the lambda form allows to share lambda
     * forms among tryFinally combinators with the same basic type.
     */
    private static LambdaForm makeTryFinallyForm(MethodType basicType) {
        MethodType lambdaType = basicType.invokerType();

        LambdaForm lform = basicType.form().cachedLambdaForm(MethodTypeForm.LF_TF);
        if (lform != null) {
            return lform;
        }
        final int THIS_MH      = 0;  // the BMH_LLLL
        final int ARG_BASE     = 1;  // start of incoming arguments
        final int ARG_LIMIT    = ARG_BASE + basicType.parameterCount();

        int nameCursor = ARG_LIMIT;
        final int GET_TARGET       = nameCursor++;
        final int GET_CLEANUP      = nameCursor++;
        final int GET_COLLECT_ARGS = nameCursor++;
        final int GET_UNBOX_RESULT = nameCursor++;
        final int BOXED_ARGS       = nameCursor++;
        final int TRY_FINALLY      = nameCursor++;
        final int UNBOX_RESULT     = nameCursor++;

        Name[] names = arguments(nameCursor - ARG_LIMIT, lambdaType);

        BoundMethodHandle.SpeciesData data = BoundMethodHandle.speciesData_LLLL();
        names[THIS_MH]          = names[THIS_MH].withConstraint(data);
        names[GET_TARGET]       = new Name(data.getterFunction(0), names[THIS_MH]);
        names[GET_CLEANUP]      = new Name(data.getterFunction(1), names[THIS_MH]);
        names[GET_COLLECT_ARGS] = new Name(data.getterFunction(2), names[THIS_MH]);
        names[GET_UNBOX_RESULT] = new Name(data.getterFunction(3), names[THIS_MH]);

        // t_{i}:L=MethodHandle.invokeBasic(collectArgs:L,a1:L,...);
        MethodType collectArgsType = basicType.changeReturnType(Object.class);
        MethodHandle invokeBasic = MethodHandles.basicInvoker(collectArgsType);
        Object[] args = new Object[invokeBasic.type().parameterCount()];
        args[0] = names[GET_COLLECT_ARGS];
        System.arraycopy(names, ARG_BASE, args, 1, ARG_LIMIT-ARG_BASE);
        names[BOXED_ARGS] = new Name(makeIntrinsic(invokeBasic, Intrinsic.TRY_FINALLY), args);

        // t_{i+1}:L=MethodHandleImpl.tryFinally(target:L,exType:L,catcher:L,t_{i}:L);
        Object[] tfArgs = new Object[] {names[GET_TARGET], names[GET_CLEANUP], names[BOXED_ARGS]};
        names[TRY_FINALLY] = new Name(NF_tryFinally, tfArgs);

        // t_{i+2}:I=MethodHandle.invokeBasic(unbox:L,t_{i+1}:L);
        MethodHandle invokeBasicUnbox = MethodHandles.basicInvoker(MethodType.methodType(basicType.rtype(), Object.class));
        Object[] unboxArgs  = new Object[] {names[GET_UNBOX_RESULT], names[TRY_FINALLY]};
        names[UNBOX_RESULT] = new Name(invokeBasicUnbox, unboxArgs);

        lform = new LambdaForm("tryFinally", lambdaType.parameterCount(), names);

        return basicType.form().setCachedLambdaForm(MethodTypeForm.LF_TF, lform);
    }

    /**
     * Intrinsified during LambdaForm compilation
     * (see {@link InvokerBytecodeGenerator#emitTryFinally emitTryFinally}).
     */
    @LambdaForm.Hidden
    static Object tryFinally(MethodHandle target, MethodHandle cleanup, Object... av) throws Throwable {
        Throwable t = null;
        Object r = null;
        try {
            // Use asFixedArity() to avoid unnecessary boxing of last argument for VarargsCollector case.
            r = target.asFixedArity().invokeWithArguments(av);
        } catch (Throwable thrown) {
            t = thrown;
            throw t;
        } finally {
            Object[] args = target.type().returnType() == void.class ? prepend(av, t) : prepend(av, t, r);
            r = cleanup.asFixedArity().invokeWithArguments(args);
        }
        return r;
    }

    // Indexes into constant method handles:
    static final int
            MH_cast                  =  0,
            MH_selectAlternative     =  1,
            MH_copyAsPrimitiveArray  =  2,
            MH_fillNewTypedArray     =  3,
            MH_fillNewArray          =  4,
            MH_arrayIdentity         =  5,
            MH_countedLoopPred       =  6,
            MH_countedLoopStep       =  7,
            MH_iteratePred           =  8,
            MH_initIterator          =  9,
            MH_iterateNext           = 10,
            MH_decrementCounter      = 11,
            MH_Array_newInstance     = 12,
            MH_LIMIT                 = 13;

    static MethodHandle getConstantHandle(int idx) {
        MethodHandle handle = HANDLES[idx];
        if (handle != null) {
            return handle;
        }
        return setCachedHandle(idx, makeConstantHandle(idx));
    }

    private static synchronized MethodHandle setCachedHandle(int idx, final MethodHandle method) {
        // Simulate a CAS, to avoid racy duplication of results.
        MethodHandle prev = HANDLES[idx];
        if (prev != null) {
            return prev;
        }
        HANDLES[idx] = method;
        return method;
    }

    // Local constant method handles:
    private static final @Stable MethodHandle[] HANDLES = new MethodHandle[MH_LIMIT];

    private static MethodHandle makeConstantHandle(int idx) {
        try {
            switch (idx) {
                case MH_cast:
                    return IMPL_LOOKUP.findVirtual(Class.class, "cast",
                            MethodType.methodType(Object.class, Object.class));
                case MH_copyAsPrimitiveArray:
                    return IMPL_LOOKUP.findStatic(MethodHandleImpl.class, "copyAsPrimitiveArray",
                            MethodType.methodType(Object.class, Wrapper.class, Object[].class));
                case MH_arrayIdentity:
                    return IMPL_LOOKUP.findStatic(MethodHandleImpl.class, "identity",
                            MethodType.methodType(Object[].class, Object[].class));
                case MH_fillNewArray:
                    return IMPL_LOOKUP.findStatic(MethodHandleImpl.class, "fillNewArray",
                            MethodType.methodType(Object[].class, Integer.class, Object[].class));
                case MH_fillNewTypedArray:
                    return IMPL_LOOKUP.findStatic(MethodHandleImpl.class, "fillNewTypedArray",
                            MethodType.methodType(Object[].class, Object[].class, Integer.class, Object[].class));
                case MH_selectAlternative:
                    return makeIntrinsic(IMPL_LOOKUP.findStatic(MethodHandleImpl.class, "selectAlternative",
                            MethodType.methodType(MethodHandle.class, boolean.class, MethodHandle.class, MethodHandle.class)),
                        Intrinsic.SELECT_ALTERNATIVE);
                case MH_countedLoopPred:
                    return IMPL_LOOKUP.findStatic(MethodHandleImpl.class, "countedLoopPredicate",
                            MethodType.methodType(boolean.class, int.class, int.class));
                case MH_countedLoopStep:
                    return IMPL_LOOKUP.findStatic(MethodHandleImpl.class, "countedLoopStep",
                            MethodType.methodType(int.class, int.class, int.class));
                case MH_iteratePred:
                    return IMPL_LOOKUP.findStatic(MethodHandleImpl.class, "iteratePredicate",
                            MethodType.methodType(boolean.class, Iterator.class));
                case MH_initIterator:
                    return IMPL_LOOKUP.findStatic(MethodHandleImpl.class, "initIterator",
                            MethodType.methodType(Iterator.class, Iterable.class));
                case MH_iterateNext:
                    return IMPL_LOOKUP.findStatic(MethodHandleImpl.class, "iterateNext",
                            MethodType.methodType(Object.class, Iterator.class));
                case MH_decrementCounter:
                    return IMPL_LOOKUP.findStatic(MethodHandleImpl.class, "decrementCounter",
                            MethodType.methodType(int.class, int.class));
                case MH_Array_newInstance:
                    return IMPL_LOOKUP.findStatic(Array.class, "newInstance",
                            MethodType.methodType(Object.class, Class.class, int.class));
            }
        } catch (ReflectiveOperationException ex) {
            throw newInternalError(ex);
        }
        throw newInternalError("Unknown function index: " + idx);
    }
}
