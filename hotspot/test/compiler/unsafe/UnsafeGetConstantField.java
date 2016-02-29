/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @summary tests on constant folding of unsafe get operations
 * @library /testlibrary /test/lib
 *
 * @requires vm.flavor != "client"
 *
 * @run main/bootclasspath -XX:+UnlockDiagnosticVMOptions
 *                   -Xbatch -XX:-TieredCompilation
 *                   -XX:+FoldStableValues
 *                   -XX:+UseUnalignedAccesses
 *                   java.lang.invoke.UnsafeGetConstantField
 * @run main/bootclasspath -XX:+UnlockDiagnosticVMOptions
 *                   -Xbatch -XX:-TieredCompilation
 *                   -XX:+FoldStableValues
 *                   -XX:-UseUnalignedAccesses
 *                   java.lang.invoke.UnsafeGetConstantField
 */
package java.lang.invoke;

import jdk.internal.vm.annotation.DontInline;
import jdk.internal.vm.annotation.Stable;
import jdk.internal.misc.Unsafe;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.FieldVisitor;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.Type;
import jdk.test.lib.Asserts;

import static jdk.internal.org.objectweb.asm.Opcodes.*;

public class UnsafeGetConstantField {
    static final Class<?> THIS_CLASS = UnsafeGetConstantField.class;

    static final Unsafe U = Unsafe.getUnsafe();

    public static void main(String[] args) {
        testUnsafeGetAddress();
        testUnsafeGetField();
        testUnsafeGetFieldUnaligned();
        System.out.println("TEST PASSED");
    }

    static final long nativeAddr = U.allocateMemory(16);
    static void testUnsafeGetAddress() {
        long cookie = 0x12345678L;
        U.putAddress(nativeAddr, cookie);
        for (int i = 0; i < 20_000; i++) {
            Asserts.assertEquals(checkGetAddress(), cookie);
        }
    }
    @DontInline
    static long checkGetAddress() {
        return U.getAddress(nativeAddr);
    }

    static void testUnsafeGetField() {
        int[] testedFlags = new int[] { 0, ACC_STATIC, ACC_FINAL, (ACC_STATIC | ACC_FINAL) };
        boolean[] boolValues = new boolean[] { false, true };
        String[] modes = new String[] { "", "Volatile" };

        for (JavaType t : JavaType.values()) {
            for (int flags : testedFlags) {
                for (boolean stable : boolValues) {
                    for (boolean hasDefaultValue : boolValues) {
                        for (String suffix : modes) {
                            runTest(t, flags, stable, hasDefaultValue, suffix);
                        }
                    }
                }
            }
        }
    }

    static void testUnsafeGetFieldUnaligned() {
        JavaType[] types = new JavaType[] { JavaType.S, JavaType.C, JavaType.I, JavaType.J };
        int[] testedFlags = new int[] { 0, ACC_STATIC, ACC_FINAL, (ACC_STATIC | ACC_FINAL) };
        boolean[] boolValues = new boolean[] { false, true };

        for (JavaType t : types) {
            for (int flags : testedFlags) {
                for (boolean stable : boolValues) {
                    for (boolean hasDefaultValue : boolValues) {
                        runTest(t, flags, stable, hasDefaultValue, "Unaligned");
                    }
                }
            }
        }
    }

    static void runTest(JavaType t, int flags, boolean stable, boolean hasDefaultValue, String postfix) {
        Generator g = new Generator(t, flags, stable, hasDefaultValue, postfix);
        Test test = g.generate();
        System.out.printf("type=%s flags=%d stable=%b default=%b post=%s\n",
                          t.typeName, flags, stable, hasDefaultValue, postfix);
        // Trigger compilation
        for (int i = 0; i < 20_000; i++) {
            Asserts.assertEQ(test.testDirect(), test.testUnsafe());
        }
    }

    interface Test {
        Object testDirect();
        Object testUnsafe();
    }

    enum JavaType {
        Z("Boolean", true),
        B("Byte", new Byte((byte)-1)),
        S("Short", new Short((short)-1)),
        C("Char", Character.MAX_VALUE),
        I("Int", -1),
        J("Long", -1L),
        F("Float", -1F),
        D("Double", -1D),
        L("Object", new Object());

        String typeName;
        Object value;
        String wrapper;
        JavaType(String name, Object value) {
            this.typeName = name;
            this.value = value;
            this.wrapper = internalName(value.getClass());
        }

        String desc() {
            if (this == JavaType.L) {
                return "Ljava/lang/Object;";
            } else {
                return toString();
            }
        }
    }

    static String internalName(Class cls) {
        return cls.getName().replace('.', '/');
    }
    static String descriptor(Class cls) {
        return String.format("L%s;", internalName(cls));
    }

    /**
     * Sample generated class:
     * static class T1 implements Test {
     *   final int f = -1;
     *   static final long FIELD_OFFSET;
     *   static final T1 t = new T1();
     *   static {
     *     FIELD_OFFSET = U.objectFieldOffset(T1.class.getDeclaredField("f"));
     *   }
     *   public Object testDirect()  { return t.f; }
     *   public Object testUnsafe()  { return U.getInt(t, FIELD_OFFSET); }
     * }
     */
    static class Generator {
        static final String FIELD_NAME = "f";
        static final String UNSAFE_NAME = internalName(Unsafe.class);
        static final String UNSAFE_DESC = descriptor(Unsafe.class);

        final JavaType type;
        final int flags;
        final boolean stable;
        final boolean hasDefaultValue;
        final String nameSuffix;

        final String className;
        final String classDesc;
        final String fieldDesc;

        Generator(JavaType t, int flags, boolean stable, boolean hasDefaultValue, String suffix) {
            this.type = t;
            this.flags = flags;
            this.stable = stable;
            this.hasDefaultValue = hasDefaultValue;
            this.nameSuffix = suffix;

            fieldDesc = type.desc();
            className = String.format("%s$Test%s%s__f=%d__s=%b__d=%b", internalName(THIS_CLASS), type.typeName,
                                      suffix, flags, stable, hasDefaultValue);
            classDesc = String.format("L%s;", className);
        }

        byte[] generateClassFile() {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, className, null, "java/lang/Object",
                    new String[]{ internalName(Test.class) });

            // Declare fields
            cw.visitField(ACC_FINAL | ACC_STATIC, "t", classDesc, null, null).visitEnd();
            cw.visitField(ACC_FINAL | ACC_STATIC, "FIELD_OFFSET", "J", null, null).visitEnd();
            cw.visitField(ACC_FINAL | ACC_STATIC, "U", UNSAFE_DESC, null, null).visitEnd();
            if (isStatic()) {
                cw.visitField(ACC_FINAL | ACC_STATIC, "STATIC_BASE", "Ljava/lang/Object;", null, null).visitEnd();
            }

            FieldVisitor fv = cw.visitField(flags, FIELD_NAME, fieldDesc, null, null);
            if (stable) {
                fv.visitAnnotation(descriptor(Stable.class), true);
            }
            fv.visitEnd();

            // Methods
            {   // <init>
                MethodVisitor mv = cw.visitMethod(0, "<init>", "()V", null, null);
                mv.visitCode();

                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
                if (!isStatic()) {
                    initField(mv);
                }
                mv.visitInsn(RETURN);

                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }

            {   // public Object testDirect() { return t.f; }
                MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "testDirect", "()Ljava/lang/Object;", null, null);
                mv.visitCode();

                getFieldValue(mv);
                wrapResult(mv);
                mv.visitInsn(ARETURN);

                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }

            {   // public Object testUnsafe() { return U.getInt(t, FIELD_OFFSET); }
                MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "testUnsafe", "()Ljava/lang/Object;", null, null);
                mv.visitCode();

                getFieldValueUnsafe(mv);
                wrapResult(mv);
                mv.visitInsn(ARETURN);

                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }

            {   // <clinit>
                MethodVisitor mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
                mv.visitCode();

                // Cache Unsafe instance
                mv.visitMethodInsn(INVOKESTATIC, UNSAFE_NAME, "getUnsafe", "()"+UNSAFE_DESC, false);
                mv.visitFieldInsn(PUTSTATIC, className, "U", UNSAFE_DESC);

                // Create test object instance
                mv.visitTypeInsn(NEW, className);
                mv.visitInsn(DUP);
                mv.visitMethodInsn(INVOKESPECIAL, className, "<init>", "()V", false);
                mv.visitFieldInsn(PUTSTATIC, className, "t", classDesc);

                // Compute field offset
                getUnsafe(mv);
                getField(mv);
                mv.visitMethodInsn(INVOKEVIRTUAL, UNSAFE_NAME, (isStatic() ? "staticFieldOffset" : "objectFieldOffset"),
                        "(Ljava/lang/reflect/Field;)J", false);
                mv.visitFieldInsn(PUTSTATIC, className, "FIELD_OFFSET", "J");

                // Compute base offset for static field
                if (isStatic()) {
                    getUnsafe(mv);
                    getField(mv);
                    mv.visitMethodInsn(INVOKEVIRTUAL, UNSAFE_NAME, "staticFieldBase", "(Ljava/lang/reflect/Field;)Ljava/lang/Object;", false);
                    mv.visitFieldInsn(PUTSTATIC, className, "STATIC_BASE", "Ljava/lang/Object;");
                    initField(mv);
                }

                mv.visitInsn(RETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }

            return cw.toByteArray();
        }

        Test generate() {
            byte[] classFile = generateClassFile();
            Class<?> c = U.defineClass(className, classFile, 0, classFile.length, THIS_CLASS.getClassLoader(), null);
            try {
                return (Test) c.newInstance();
            } catch(Exception e) {
                throw new Error(e);
            }
        }

        boolean isStatic() {
            return (flags & ACC_STATIC) > 0;
        }
        boolean isFinal() {
            return (flags & ACC_FINAL) > 0;
        }
        void getUnsafe(MethodVisitor mv) {
            mv.visitFieldInsn(GETSTATIC, className, "U", UNSAFE_DESC);
        }
        void getField(MethodVisitor mv) {
            mv.visitLdcInsn(Type.getType(classDesc));
            mv.visitLdcInsn(FIELD_NAME);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredField", "(Ljava/lang/String;)Ljava/lang/reflect/Field;", false);
        }
        void getFieldValue(MethodVisitor mv) {
            if (isStatic()) {
                mv.visitFieldInsn(GETSTATIC, className, FIELD_NAME, fieldDesc);
            } else {
                mv.visitFieldInsn(GETSTATIC, className, "t", classDesc);
                mv.visitFieldInsn(GETFIELD, className, FIELD_NAME, fieldDesc);
            }
        }
        void getFieldValueUnsafe(MethodVisitor mv) {
            getUnsafe(mv);
            if (isStatic()) {
                mv.visitFieldInsn(GETSTATIC, className, "STATIC_BASE", "Ljava/lang/Object;");
            } else {
                mv.visitFieldInsn(GETSTATIC, className, "t", classDesc);
            }
            mv.visitFieldInsn(GETSTATIC, className, "FIELD_OFFSET", "J");
            String name = "get" + type.typeName + nameSuffix;
            mv.visitMethodInsn(INVOKEVIRTUAL, UNSAFE_NAME, name, "(Ljava/lang/Object;J)" + type.desc(), false);
        }
        void wrapResult(MethodVisitor mv) {
            if (type != JavaType.L) {
                String desc = String.format("(%s)L%s;", type.desc(), type.wrapper);
                mv.visitMethodInsn(INVOKESTATIC, type.wrapper, "valueOf", desc, false);
            }
        }
        void initField(MethodVisitor mv) {
            if (hasDefaultValue) {
                return; // Nothing to do
            }
            if (!isStatic()) {
                mv.visitVarInsn(ALOAD, 0);
            }
            switch (type) {
                case L: {
                    mv.visitTypeInsn(NEW, "java/lang/Object");
                    mv.visitInsn(DUP);
                    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);

                    break;
                }
                default: {
                    mv.visitLdcInsn(type.value);
                    break;
                }
            }
            mv.visitFieldInsn((isStatic() ? PUTSTATIC : PUTFIELD), className, FIELD_NAME, fieldDesc);
        }
    }
}
