/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

package jdk.incubator.http.internal.websocket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.concurrent.atomic.AtomicLong;

/*
 * Receives incoming data from the channel on demand and converts it into a
 * stream of WebSocket messages which are then delivered to the supplied message
 * consumer in a strict sequential order and non-recursively. In other words,
 *
 *     onText()
 *     onText()
 *     onBinary()
 *     ...
 *
 * instead of
 *
 *     onText()
 *       onText()
 *         onBinary()
 *     ...
 *
 * even if `request(long n)` is called from inside these invocations.
 */
final class Receiver {

    private final MessageStreamConsumer messageConsumer;
    private final RawChannel channel;
    private final FrameConsumer frameConsumer;
    private final Frame.Reader reader = new Frame.Reader();
    private final RawChannel.RawEvent event = createHandler();
    private final AtomicLong demand = new AtomicLong();
    private final CooperativeHandler handler;

    private ByteBuffer data;
    private volatile int state;

    private static final int UNREGISTERED = 0;
    private static final int AVAILABLE    = 1;
    private static final int WAITING      = 2;

    Receiver(MessageStreamConsumer messageConsumer, RawChannel channel) {
        this.messageConsumer = messageConsumer;
        this.channel = channel;
        this.frameConsumer = new FrameConsumer(this.messageConsumer);
        this.data = channel.initialByteBuffer();
        // To ensure the initial non-final `data` will be visible
        // (happens-before) when `handler` invokes `pushContinuously`
        // the following assignment is done last:
        handler = new CooperativeHandler(this::pushContinuously);
    }

    private RawChannel.RawEvent createHandler() {
        return new RawChannel.RawEvent() {

            @Override
            public int interestOps() {
                return SelectionKey.OP_READ;
            }

            @Override
            public void handle() {
                state = AVAILABLE;
                handler.handle();
            }
        };
    }

    void request(long n) {
        if (n < 0L) {
            throw new IllegalArgumentException("Negative: " + n);
        }
        demand.accumulateAndGet(n, (p, i) -> p + i < 0 ? Long.MAX_VALUE : p + i);
        handler.handle();
    }

    void acknowledge() {
        long x = demand.decrementAndGet();
        if (x < 0) {
            throw new InternalError(String.valueOf(x));
        }
    }

    /*
     * Stops the machinery from reading and delivering messages permanently,
     * regardless of the current demand and data availability.
     */
    void close() {
        handler.stop();
    }

    private void pushContinuously() {
        while (!handler.isStopped()) {
            if (data.hasRemaining()) {
                if (demand.get() > 0) {
                    try {
                        int oldPos = data.position();
                        reader.readFrame(data, frameConsumer);
                        int newPos = data.position();
                        assert oldPos != newPos : data; // reader always consumes bytes
                    } catch (FailWebSocketException e) {
                        handler.stop();
                        messageConsumer.onError(e);
                    }
                    continue;
                }
                break;
            }
            switch (state) {
                case WAITING:
                    return;
                case UNREGISTERED:
                    try {
                        state = WAITING;
                        channel.registerEvent(event);
                    } catch (IOException e) {
                        handler.stop();
                        messageConsumer.onError(e);
                    }
                    return;
                case AVAILABLE:
                    try {
                        data = channel.read();
                    } catch (IOException e) {
                        handler.stop();
                        messageConsumer.onError(e);
                        return;
                    }
                    if (data == null) { // EOF
                        handler.stop();
                        messageConsumer.onComplete();
                        return;
                    } else if (!data.hasRemaining()) { // No data at the moment
                        // Pretty much a "goto", reusing the existing code path
                        // for registration
                        state = UNREGISTERED;
                    }
                    continue;
                default:
                    throw new InternalError(String.valueOf(state));
            }
        }
    }
}

