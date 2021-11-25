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
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <jni.h>
#include <jvmti.h>
#include <aod.h>
#include <jvmti_aod.h>

extern "C" {

/*
 * Expected agent work scenario:
 *  - receive MonitorContentedEnter event for thread ThreadGeneratingEvents
 *  - receive MonitorContentedEntered event for thread ThreadGeneratingEvents and finish work
 */

#define THREAD_GENERATING_EVENTS_NAME "ThreadGeneratingEvents"

static Options* options = NULL;
static const char* agentName;

static jvmtiEvent testEvents[] = {JVMTI_EVENT_MONITOR_WAIT, JVMTI_EVENT_MONITOR_WAITED};
static const int testEventsNumber = 2;

volatile int monitorWaitReceived = 0;

void JNICALL monitorWaitHandler(jvmtiEnv *jvmti,
        JNIEnv* jni,
        jthread thread,
        jobject object,
        jlong timeout) {
    char threadName[MAX_STRING_LENGTH];

    if (!nsk_jvmti_aod_getThreadName(jvmti, thread, threadName)) {
        nsk_jvmti_aod_disableEventsAndFinish(agentName, testEvents, testEventsNumber, 0, jvmti, jni);
        return;
    }

    NSK_DISPLAY2("%s: MonitorWait event was received for thread '%s'\n", agentName, threadName);

    if (!strcmp(THREAD_GENERATING_EVENTS_NAME, threadName)) {
        monitorWaitReceived = 1;
    }
}

void JNICALL monitorWaitedHandler(jvmtiEnv *jvmti,
        JNIEnv* jni,
        jthread thread,
        jobject object,
        jboolean timed_out) {
    char threadName[MAX_STRING_LENGTH];

    if (!nsk_jvmti_aod_getThreadName(jvmti, thread, threadName)) {
        nsk_jvmti_aod_disableEventsAndFinish(agentName, testEvents, testEventsNumber, 0, jvmti, jni);
        return;
    }

    NSK_DISPLAY2("%s: MonitorWaited event was received for thread '%s'\n", agentName, threadName);

    if (!strcmp(THREAD_GENERATING_EVENTS_NAME, threadName)) {
        int success = 1;

        if (!monitorWaitReceived) {
            success = 0;
            NSK_COMPLAIN2("%s: MonitorWait event wasn't received for thread '%s'\n", agentName, threadName);
        }

        nsk_jvmti_aod_disableEventsAndFinish(agentName, testEvents, testEventsNumber, success, jvmti, jni);
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNI_OnLoad_attach037Agent00(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif

JNIEXPORT jint JNICALL
#ifdef STATIC_BUILD
Agent_OnAttach_attach037Agent00(JavaVM *vm, char *optionsString, void *reserved)
#else
Agent_OnAttach(JavaVM *vm, char *optionsString, void *reserved)
#endif
{
    jvmtiEventCallbacks eventCallbacks;
    jvmtiCapabilities caps;
    jvmtiEnv* jvmti;
    JNIEnv* jni;

    if (!NSK_VERIFY((options = (Options*) nsk_aod_createOptions(optionsString)) != NULL))
        return JNI_ERR;

    agentName = nsk_aod_getOptionValue(options, NSK_AOD_AGENT_NAME_OPTION);

    if ((jni = (JNIEnv*) nsk_aod_createJNIEnv(vm)) == NULL)
        return JNI_ERR;

    if (!NSK_VERIFY((jvmti = nsk_jvmti_createJVMTIEnv(vm, reserved)) != NULL))
        return JNI_ERR;

    memset(&caps, 0, sizeof(caps));
    caps.can_generate_monitor_events = 1;
    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps)) ) {
        return JNI_ERR;
    }

    memset(&eventCallbacks,0, sizeof(eventCallbacks));
    eventCallbacks.MonitorWaited = monitorWaitedHandler;
    eventCallbacks.MonitorWait = monitorWaitHandler;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&eventCallbacks, sizeof(eventCallbacks))) ) {
        return JNI_ERR;
    }

    if (!(nsk_jvmti_aod_enableEvents(jvmti, testEvents, testEventsNumber))) {
        return JNI_ERR;
    }

    NSK_DISPLAY1("%s: initialization was done\n", agentName);

    if (!NSK_VERIFY(nsk_aod_agentLoaded(jni, agentName)))
        return JNI_ERR;

    return JNI_OK;
}

}
