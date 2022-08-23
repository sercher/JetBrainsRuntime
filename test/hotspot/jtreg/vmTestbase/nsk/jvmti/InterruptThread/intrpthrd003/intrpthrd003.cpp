/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include <stdio.h>
#include <string.h>
#include "jvmti.h"
#include "jni_tools.h"
#include "agent_common.h"
#include "JVMTITools.h"

extern "C" {


#define PASSED 0
#define STATUS_FAILED 2

static jvmtiEnv *jvmti;
static jvmtiCapabilities caps;
static jint result = PASSED;
static jvmtiError intrpthrd_err = JVMTI_ERROR_NONE;

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_intrpthrd003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_intrpthrd003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_intrpthrd003(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint  Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res;
    jvmtiError err;

    res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (res != JNI_OK || jvmti == NULL) {
        printf("Wrong result of a valid call to GetEnv!\n");
        return JNI_ERR;
    }

    err = jvmti->GetPotentialCapabilities(&caps);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetPotentialCapabilities) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    err = jvmti->AddCapabilities(&caps);
    if (err != JVMTI_ERROR_NONE) {
        printf("(AddCapabilities) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    err = jvmti->GetCapabilities(&caps);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetCapabilities) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    if (!caps.can_signal_thread) {
        printf("Warning: InterruptThread is not implemented\n");
    }

    return JNI_OK;
}


JNIEXPORT jint JNICALL
Java_nsk_jvmti_InterruptThread_intrpthrd003_check (JNIEnv *env, jobject oobj,
        jlong ind, jthread thr) {

    intrpthrd_err = jvmti->InterruptThread(thr);
    if (intrpthrd_err == JVMTI_ERROR_MUST_POSSESS_CAPABILITY &&
            !caps.can_signal_thread) {
        /* It is OK */
    } else {
        switch (intrpthrd_err) {
        case JVMTI_ERROR_NONE:
        case JVMTI_ERROR_THREAD_NOT_ALIVE:
            break;

        default:
            printf("(thr#%" LL "d) error expected: JVMTI_ERROR_NONE or JVMTI_ERROR_THREAD_NOT_ALIVE,", ind);
            printf(" got: %s (%d)\n", TranslateError(intrpthrd_err), intrpthrd_err);
            result = STATUS_FAILED;
            break;
        }
    }

    return result;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_InterruptThread_intrpthrd003_getResult (JNIEnv *env, jclass cls) {
    return result;
}

JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_InterruptThread_intrpthrd003_isThreadNotAliveError(JNIEnv *env, jclass cls) {
    if (intrpthrd_err == JVMTI_ERROR_THREAD_NOT_ALIVE) {
        return JNI_TRUE;
    }
    return JNI_FALSE;
}

}
