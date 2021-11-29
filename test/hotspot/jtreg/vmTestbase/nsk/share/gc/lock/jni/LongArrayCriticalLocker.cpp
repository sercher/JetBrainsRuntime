/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include <jni.h>
#include <stdio.h>
#include <time.h>
#include "ExceptionCheckingJniEnv.hpp"
#include "jni_tools.h"

extern "C" {

static jfieldID objFieldId = NULL;

/*
 * Class:     nsk_share_gc_lock_jni_LongArrayCriticalLocker
 * Method:    criticalNative
 * Signature: ([Z)Z
 */
JNIEXPORT jlong JNICALL Java_nsk_share_gc_lock_jni_LongArrayCriticalLocker_criticalNative
(JNIEnv *jni_env, jobject o, jlong enterTime, jlong sleepTime) {
        ExceptionCheckingJniEnvPtr env(jni_env);

        jsize size, i;
        jlongArray arr;
        jlong *pa;
        jlong hash = 0;
        time_t start_time, current_time;

        if (objFieldId == NULL) {
                jclass klass = env->GetObjectClass(o);
                objFieldId = env->GetFieldID(klass, "obj", "Ljava/lang/Object;");
        }
        arr = (jlongArray) env->GetObjectField(o, objFieldId);
        env->SetObjectField(o, objFieldId, NULL);

        size = env->GetArrayLength(arr);
        start_time = time(NULL);
        enterTime /= 1000;
        current_time = 0;
        while (current_time - start_time < enterTime) {
                pa = (jlong*) env->GetPrimitiveArrayCritical(arr, NULL);
                if (pa != NULL) {
                        for (i = 0; i < size; ++i)
                                hash ^= pa[i];
                } else {
                        hash = 0;
                }
                mssleep((long) sleepTime);
                env->ReleasePrimitiveArrayCritical(arr, pa, 0);
                mssleep((long) sleepTime);
                current_time = time(NULL);
        }
        env->SetObjectField(o, objFieldId, arr);
        return hash;
}

}
