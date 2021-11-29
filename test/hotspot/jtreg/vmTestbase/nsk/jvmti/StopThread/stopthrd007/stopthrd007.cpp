/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include <stdlib.h>
#include <string.h>
#include "jni_tools.h"
#include "agent_common.h"
#include "jvmti_tools.h"

#define PASSED 0
#define STATUS_FAILED 2

extern "C" {

/* ========================================================================== */

/* scaffold objects */
static jlong timeout = 0;

/* test objects */
static jobject threadDeath = NULL;
static jthread runningThread = NULL;
static jthread waitingThread = NULL;
static jthread sleepingThread = NULL;

/* ========================================================================== */

static int prepare(jvmtiEnv* jvmti, JNIEnv* jni) {
    const char* RUNNING_THREAD_NAME = "DebuggeeRunningThread";
    const char* WAITING_THREAD_NAME = "DebuggeeWaitingThread";
    const char* SLEEPING_THREAD_NAME = "DebuggeeSleepingThread";
    const char* THREAD_DEATH_CLASS_NAME = "java/lang/ThreadDeath";
    const char* THREAD_DEATH_CTOR_NAME = "<init>";
    const char* THREAD_DEATH_CTOR_SIGNATURE = "()V";
    jvmtiThreadInfo info;
    jthread *threads = NULL;
    jint threads_count = 0;
    jclass cls = NULL;
    jmethodID ctor = NULL;
    int i;

    NSK_DISPLAY0("Prepare: find tested threads\n");

    /* get all live threads */
    if (!NSK_JVMTI_VERIFY(jvmti->GetAllThreads(&threads_count, &threads)))
        return NSK_FALSE;

    if (!NSK_VERIFY(threads_count > 0 && threads != NULL))
        return NSK_FALSE;

    /* find tested thread */
    for (i = 0; i < threads_count; i++) {
        if (!NSK_VERIFY(threads[i] != NULL))
            return NSK_FALSE;

        /* get thread information */
        if (!NSK_JVMTI_VERIFY(jvmti->GetThreadInfo(threads[i], &info)))
            return NSK_FALSE;

        NSK_DISPLAY3("    thread #%d (%s): %p\n", i, info.name, threads[i]);

        /* find by name */
        if (info.name != NULL) {
            if (strcmp(info.name, RUNNING_THREAD_NAME) == 0) {
                runningThread = threads[i];
            } else if (strcmp(info.name, WAITING_THREAD_NAME) == 0) {
                waitingThread = threads[i];
            } else if (strcmp(info.name, SLEEPING_THREAD_NAME) == 0) {
                sleepingThread = threads[i];
            }
        }
    }

    if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*)threads)))
        return NSK_FALSE;

    NSK_DISPLAY0("Prepare: create new instance of ThreadDeath exception\n");

    if (!NSK_JNI_VERIFY(jni, (cls = jni->FindClass(THREAD_DEATH_CLASS_NAME)) != NULL))
        return NSK_FALSE;

    if (!NSK_JNI_VERIFY(jni, (ctor =
            jni->GetMethodID(cls, THREAD_DEATH_CTOR_NAME, THREAD_DEATH_CTOR_SIGNATURE)) != NULL))
        return NSK_FALSE;

    if (!NSK_JNI_VERIFY(jni, (threadDeath = jni->NewObject(cls, ctor)) != NULL))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* ========================================================================== */

/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {

    if (!nsk_jvmti_waitForSync(timeout))
        return;

    if (!prepare(jvmti, jni)) {
        nsk_jvmti_setFailStatus();
        return;
    }

    NSK_DISPLAY0("Testcase #1: call StopThread for runningThread\n");
    if (!NSK_VERIFY(runningThread != NULL)) {
        nsk_jvmti_setFailStatus();
    } else {
        if (!NSK_JVMTI_VERIFY(jvmti->StopThread(runningThread, threadDeath)))
            nsk_jvmti_setFailStatus();
    }

    NSK_DISPLAY0("Testcase #2: call StopThread for waitingThread\n");
    if (!NSK_VERIFY(waitingThread != NULL)) {
        nsk_jvmti_setFailStatus();
    } else {
        if (!NSK_JVMTI_VERIFY(jvmti->StopThread(waitingThread, threadDeath)))
            nsk_jvmti_setFailStatus();
    }

    NSK_DISPLAY0("Testcase #3: call StopThread for sleepingThread\n");
    if (!NSK_VERIFY(sleepingThread != NULL)) {
        nsk_jvmti_setFailStatus();
    } else {
        if (!NSK_JVMTI_VERIFY(jvmti->StopThread(sleepingThread, threadDeath)))
            nsk_jvmti_setFailStatus();
    }

    if (!nsk_jvmti_resumeSync())
        return;
}

/* ========================================================================== */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_stopthrd007(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_stopthrd007(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_stopthrd007(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiEnv* jvmti = NULL;
    jvmtiCapabilities caps;

    NSK_DISPLAY0("Agent_OnLoad\n");

    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60 * 1000;

    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    memset(&caps, 0, sizeof(caps));
    caps.can_signal_thread = 1;
    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps))) {
        return JNI_ERR;
    }

    return JNI_OK;
}

/* ========================================================================== */

}
