/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include "jni.h"
#include "jni_util.h"
#include "jvm.h"
#include "jlong.h"
#include "nio.h"
#include "nio_util.h"
#include "wepoll.h"
#include "sun_nio_ch_WEPoll.h"

JNIEXPORT jint JNICALL
Java_sun_nio_ch_WEPoll_eventSize(JNIEnv* env, jclass clazz)
{
    return sizeof(struct epoll_event);
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_WEPoll_eventsOffset(JNIEnv* env, jclass clazz)
{
    return offsetof(struct epoll_event, events);
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_WEPoll_dataOffset(JNIEnv* env, jclass clazz)
{
    return offsetof(struct epoll_event, data);
}

JNIEXPORT jlong JNICALL
Java_sun_nio_ch_WEPoll_create(JNIEnv *env, jclass clazz) {
    HANDLE h = epoll_create1(0);
    if (h == NULL) {
        JNU_ThrowIOExceptionWithLastError(env, "epoll_create1 failed");
    }
    return ptr_to_jlong(h);
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_WEPoll_ctl(JNIEnv *env, jclass clazz, jlong h,
                           jint opcode, jlong s, jint events)
{
    struct epoll_event event;
    int res;
    SOCKET socket = (SOCKET) jlong_to_ptr(s);

    event.events = (uint32_t) events;
    event.data.sock = socket;

    res = epoll_ctl(jlong_to_ptr(h), opcode, socket, &event);
    return (res == 0) ? 0 : errno;
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_WEPoll_wait(JNIEnv *env, jclass clazz, jlong h,
                            jlong address, jint numfds, jint timeout)
{
    struct epoll_event *events = jlong_to_ptr(address);
    int res = epoll_wait(jlong_to_ptr(h), events, numfds, timeout);
    if (res < 0) {
        JNU_ThrowIOExceptionWithLastError(env, "epoll_wait failed");
        return IOS_THROWN;
    }
    return res;
}

JNIEXPORT void JNICALL
Java_sun_nio_ch_WEPoll_close(JNIEnv *env, jclass clazz, jlong h) {
    epoll_close(jlong_to_ptr(h));
}
