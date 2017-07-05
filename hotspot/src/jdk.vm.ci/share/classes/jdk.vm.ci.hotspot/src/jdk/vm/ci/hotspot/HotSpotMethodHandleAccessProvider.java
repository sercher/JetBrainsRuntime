/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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
package jdk.vm.ci.hotspot;

import static jdk.vm.ci.hotspot.CompilerToVM.compilerToVM;
import static jdk.vm.ci.hotspot.HotSpotJVMCIRuntime.runtime;
import static jdk.vm.ci.hotspot.HotSpotResolvedObjectTypeImpl.fromObjectClass;
import jdk.vm.ci.common.JVMCIError;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.MethodHandleAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

public class HotSpotMethodHandleAccessProvider implements MethodHandleAccessProvider, HotSpotProxified {

    private final ConstantReflectionProvider constantReflection;

    public HotSpotMethodHandleAccessProvider(ConstantReflectionProvider constantReflection) {
        this.constantReflection = constantReflection;
    }

    /**
     * Lazy initialization to break class initialization cycle. Field and method lookup is only
     * possible after the {@link HotSpotJVMCIRuntime} is fully initialized.
     */
    static class LazyInitialization {
        static final ResolvedJavaField methodHandleFormField;
        static final ResolvedJavaField lambdaFormVmentryField;
        static final ResolvedJavaMethod lambdaFormCompileToBytecodeMethod;
        static final HotSpotResolvedJavaField memberNameVmtargetField;

        /**
         * Search for an instance field with the given name in a class.
         *
         * @param className name of the class to search in
         * @param fieldName name of the field to be searched
         * @return resolved java field
         * @throws ClassNotFoundException
         */
        private static ResolvedJavaField findFieldInClass(String className, String fieldName) throws ClassNotFoundException {
            Class<?> clazz = Class.forName(className);
            ResolvedJavaType type = runtime().fromClass(clazz);
            ResolvedJavaField[] fields = type.getInstanceFields(false);
            for (ResolvedJavaField field : fields) {
                if (field.getName().equals(fieldName)) {
                    return field;
                }
            }
            return null;
        }

        private static ResolvedJavaMethod findMethodInClass(String className, String methodName) throws ClassNotFoundException {
            Class<?> clazz = Class.forName(className);
            HotSpotResolvedObjectTypeImpl type = fromObjectClass(clazz);
            ResolvedJavaMethod result = null;
            for (ResolvedJavaMethod method : type.getDeclaredMethods()) {
                if (method.getName().equals(methodName)) {
                    assert result == null : "more than one method found: " + className + "." + methodName;
                    result = method;
                }
            }
            assert result != null : "method not found: " + className + "." + methodName;
            return result;
        }

        static {
            try {
                methodHandleFormField = findFieldInClass("java.lang.invoke.MethodHandle", "form");
                lambdaFormVmentryField = findFieldInClass("java.lang.invoke.LambdaForm", "vmentry");
                lambdaFormCompileToBytecodeMethod = findMethodInClass("java.lang.invoke.LambdaForm", "compileToBytecode");
                memberNameVmtargetField = (HotSpotResolvedJavaField) findFieldInClass("java.lang.invoke.MemberName", "vmtarget");
            } catch (Throwable ex) {
                throw new JVMCIError(ex);
            }
        }
    }

    @Override
    public IntrinsicMethod lookupMethodHandleIntrinsic(ResolvedJavaMethod method) {
        int intrinsicId = ((HotSpotResolvedJavaMethodImpl) method).intrinsicId();
        if (intrinsicId != 0) {
            return getMethodHandleIntrinsic(intrinsicId);
        }
        return null;
    }

    public static IntrinsicMethod getMethodHandleIntrinsic(int intrinsicId) {
        HotSpotVMConfig config = runtime().getConfig();
        if (intrinsicId == config.vmIntrinsicInvokeBasic) {
            return IntrinsicMethod.INVOKE_BASIC;
        } else if (intrinsicId == config.vmIntrinsicLinkToInterface) {
            return IntrinsicMethod.LINK_TO_INTERFACE;
        } else if (intrinsicId == config.vmIntrinsicLinkToSpecial) {
            return IntrinsicMethod.LINK_TO_SPECIAL;
        } else if (intrinsicId == config.vmIntrinsicLinkToStatic) {
            return IntrinsicMethod.LINK_TO_STATIC;
        } else if (intrinsicId == config.vmIntrinsicLinkToVirtual) {
            return IntrinsicMethod.LINK_TO_VIRTUAL;
        }
        return null;
    }

    @Override
    public ResolvedJavaMethod resolveInvokeBasicTarget(JavaConstant methodHandle, boolean forceBytecodeGeneration) {
        if (methodHandle.isNull()) {
            return null;
        }

        /* Load non-public field: LambdaForm MethodHandle.form */
        JavaConstant lambdaForm = constantReflection.readFieldValue(LazyInitialization.methodHandleFormField, methodHandle);
        if (lambdaForm == null || lambdaForm.isNull()) {
            return null;
        }

        JavaConstant memberName;
        if (forceBytecodeGeneration) {
            /* Invoke non-public method: MemberName LambdaForm.compileToBytecode() */
            memberName = LazyInitialization.lambdaFormCompileToBytecodeMethod.invoke(lambdaForm, new JavaConstant[0]);
        } else {
            /* Load non-public field: MemberName LambdaForm.vmentry */
            memberName = constantReflection.readFieldValue(LazyInitialization.lambdaFormVmentryField, lambdaForm);
        }
        return getTargetMethod(memberName);
    }

    @Override
    public ResolvedJavaMethod resolveLinkToTarget(JavaConstant memberName) {
        return getTargetMethod(memberName);
    }

    /**
     * Returns the {@link ResolvedJavaMethod} for the vmtarget of a java.lang.invoke.MemberName.
     */
    private static ResolvedJavaMethod getTargetMethod(JavaConstant memberName) {
        if (memberName.isNull()) {
            return null;
        }

        Object object = ((HotSpotObjectConstantImpl) memberName).object();
        /* Read the ResolvedJavaMethod from the injected field MemberName.vmtarget */
        return compilerToVM().getResolvedJavaMethod(object, LazyInitialization.memberNameVmtargetField.offset());
    }
}
