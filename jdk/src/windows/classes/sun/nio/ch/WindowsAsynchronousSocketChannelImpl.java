/*
 * Copyright 2008-2009 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package sun.nio.ch;

import java.nio.channels.*;
import java.nio.ByteBuffer;
import java.nio.BufferOverflowException;
import java.net.*;
import java.util.concurrent.*;
import java.io.IOException;
import sun.misc.Unsafe;

/**
 * Windows implementation of AsynchronousSocketChannel using overlapped I/O.
 */

class WindowsAsynchronousSocketChannelImpl
    extends AsynchronousSocketChannelImpl implements Iocp.OverlappedChannel
{
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static int addressSize = unsafe.addressSize();

    private static int dependsArch(int value32, int value64) {
        return (addressSize == 4) ? value32 : value64;
    }

    /*
     * typedef struct _WSABUF {
     *     u_long      len;
     *     char FAR *  buf;
     * } WSABUF;
     */
    private static final int SIZEOF_WSABUF  = dependsArch(8, 16);
    private static final int OFFSETOF_LEN   = 0;
    private static final int OFFSETOF_BUF   = dependsArch(4, 8);

    // maximum vector size for scatter/gather I/O
    private static final int MAX_WSABUF     = 16;

    private static final int SIZEOF_WSABUFARRAY = MAX_WSABUF * SIZEOF_WSABUF;


    // socket handle. Use begin()/end() around each usage of this handle.
    final long handle;

    // I/O completion port that the socket is associated with
    private final Iocp iocp;

    // completion key to identify channel when I/O completes
    private final int completionKey;

    // Pending I/O operations are tied to an OVERLAPPED structure that can only
    // be released when the I/O completion event is posted to the completion
    // port. Where I/O operations complete immediately then it is possible
    // there may be more than two OVERLAPPED structures in use.
    private final PendingIoCache ioCache;

    // per-channel arrays of WSABUF structures
    private final long readBufferArray;
    private final long writeBufferArray;


    WindowsAsynchronousSocketChannelImpl(Iocp iocp, boolean failIfGroupShutdown)
        throws IOException
    {
        super(iocp);

        // associate socket with default completion port
        long h = IOUtil.fdVal(fd);
        int key = 0;
        try {
            key = iocp.associate(this, h);
        } catch (ShutdownChannelGroupException x) {
            if (failIfGroupShutdown) {
                closesocket0(h);
                throw x;
            }
        } catch (IOException x) {
            closesocket0(h);
            throw x;
        }

        this.handle = h;
        this.iocp = iocp;
        this.completionKey = key;
        this.ioCache = new PendingIoCache();

        // allocate WSABUF arrays
        this.readBufferArray = unsafe.allocateMemory(SIZEOF_WSABUFARRAY);
        this.writeBufferArray = unsafe.allocateMemory(SIZEOF_WSABUFARRAY);
    }

    WindowsAsynchronousSocketChannelImpl(Iocp iocp) throws IOException {
        this(iocp, true);
    }

    @Override
    public AsynchronousChannelGroupImpl group() {
        return iocp;
    }

    /**
     * Invoked by Iocp when an I/O operation competes.
     */
    @Override
    public <V,A> PendingFuture<V,A> getByOverlapped(long overlapped) {
        return ioCache.remove(overlapped);
    }

    // invoked by WindowsAsynchronousServerSocketChannelImpl
    long handle() {
        return handle;
    }

    // invoked by WindowsAsynchronousServerSocketChannelImpl when new connection
    // accept
    void setConnected(SocketAddress localAddress, SocketAddress remoteAddress) {
        synchronized (stateLock) {
            state = ST_CONNECTED;
            this.localAddress = localAddress;
            this.remoteAddress = remoteAddress;
        }
    }

    @Override
    void implClose() throws IOException {
        // close socket (may cause outstanding async I/O operations to fail).
        closesocket0(handle);

        // waits until all I/O operations have completed
        ioCache.close();

        // release arrays of WSABUF structures
        unsafe.freeMemory(readBufferArray);
        unsafe.freeMemory(writeBufferArray);

        // finally disassociate from the completion port (key can be 0 if
        // channel created when group is shutdown)
        if (completionKey != 0)
            iocp.disassociate(completionKey);
    }

    @Override
    public void onCancel(PendingFuture<?,?> task) {
        if (task.getContext() instanceof ConnectTask)
            killConnect();
        if (task.getContext() instanceof ReadTask)
            killReading();
        if (task.getContext() instanceof WriteTask)
            killWriting();
    }

    /**
     * Implements the task to initiate a connection and the handler to
     * consume the result when the connection is established (or fails).
     */
    private class ConnectTask<A> implements Runnable, Iocp.ResultHandler {
        private final InetSocketAddress remote;
        private final PendingFuture<Void,A> result;

        ConnectTask(InetSocketAddress remote, PendingFuture<Void,A> result) {
            this.remote = remote;
            this.result = result;
        }

        private void closeChannel() {
            try {
                close();
            } catch (IOException ignore) { }
        }

        private IOException toIOException(Throwable x) {
            if (x instanceof IOException) {
                if (x instanceof ClosedChannelException)
                    x = new AsynchronousCloseException();
                return (IOException)x;
            }
            return new IOException(x);
        }

        /**
         * Invoke after a connection is successfully established.
         */
        private void afterConnect() throws IOException {
            updateConnectContext(handle);
            synchronized (stateLock) {
                state = ST_CONNECTED;
                remoteAddress = remote;
            }
        }

        /**
         * Task to initiate a connection.
         */
        @Override
        public void run() {
            long overlapped = 0L;
            Throwable exc = null;
            try {
                begin();

                // synchronize on result to allow this thread handle the case
                // where the connection is established immediately.
                synchronized (result) {
                    overlapped = ioCache.add(result);
                    // initiate the connection
                    int n = connect0(handle, Net.isIPv6Available(), remote.getAddress(),
                                     remote.getPort(), overlapped);
                    if (n == IOStatus.UNAVAILABLE) {
                        // connection is pending
                        return;
                    }

                    // connection established immediately
                    afterConnect();
                    result.setResult(null);
                }
            } catch (Throwable x) {
                exc = x;
            } finally {
                end();
            }

            if (exc != null) {
                if (overlapped != 0L)
                    ioCache.remove(overlapped);
                closeChannel();
                result.setFailure(toIOException(exc));
            }
            Invoker.invoke(result.handler(), result);
        }

        /**
         * Invoked by handler thread when connection established.
         */
        @Override
        public void completed(int bytesTransferred) {
            Throwable exc = null;
            try {
                begin();
                afterConnect();
                result.setResult(null);
            } catch (Throwable x) {
                // channel is closed or unable to finish connect
                exc = x;
            } finally {
                end();
            }

            // can't close channel while in begin/end block
            if (exc != null) {
                closeChannel();
                result.setFailure(toIOException(exc));
            }

            Invoker.invoke(result.handler(), result);
        }

        /**
         * Invoked by handler thread when failed to establish connection.
         */
        @Override
        public void failed(int error, IOException x) {
            if (isOpen()) {
                closeChannel();
                result.setFailure(x);
            } else {
                result.setFailure(new AsynchronousCloseException());
            }
            Invoker.invoke(result.handler(), result);
        }
    }

    @Override
    public <A> Future<Void> connect(SocketAddress remote,
                                    A attachment,
                                    CompletionHandler<Void,? super A> handler)
    {
        if (!isOpen()) {
            CompletedFuture<Void,A> result = CompletedFuture
                .withFailure(this, new ClosedChannelException(), attachment);
            Invoker.invoke(handler, result);
            return result;
        }

        InetSocketAddress isa = Net.checkAddress(remote);

        // permission check
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            sm.checkConnect(isa.getAddress().getHostAddress(), isa.getPort());

        // check and update state
        // ConnectEx requires the socket to be bound to a local address
        IOException bindException = null;
        synchronized (stateLock) {
            if (state == ST_CONNECTED)
                throw new AlreadyConnectedException();
            if (state == ST_PENDING)
                throw new ConnectionPendingException();
            if (localAddress == null) {
                try {
                    bind(new InetSocketAddress(0));
                } catch (IOException x) {
                    bindException = x;
                }
            }
            if (bindException == null)
                state = ST_PENDING;
        }

        // handle bind failure
        if (bindException != null) {
            try {
                close();
            } catch (IOException ignore) { }
            CompletedFuture<Void,A> result = CompletedFuture
                .withFailure(this, bindException, attachment);
            Invoker.invoke(handler, result);
            return result;
        }

        // setup task
        PendingFuture<Void,A> result =
            new PendingFuture<Void,A>(this, handler, attachment);
        ConnectTask task = new ConnectTask<A>(isa, result);
        result.setContext(task);

        // initiate I/O (can only be done from thread in thread pool)
        Invoker.invokeOnThreadInThreadPool(this, task);
        return result;
    }

    /**
     * Implements the task to initiate a read and the handler to consume the
     * result when the read completes.
     */
    private class ReadTask<V,A> implements Runnable, Iocp.ResultHandler {
        private final ByteBuffer[] bufs;
        private final int numBufs;
        private final boolean scatteringRead;
        private final PendingFuture<V,A> result;

        // set by run method
        private ByteBuffer[] shadow;

        ReadTask(ByteBuffer[] bufs,
                 boolean scatteringRead,
                 PendingFuture<V,A> result)
        {
            this.bufs = bufs;
            this.numBufs = (bufs.length > MAX_WSABUF) ? MAX_WSABUF : bufs.length;
            this.scatteringRead = scatteringRead;
            this.result = result;
        }

        /**
         * Invoked prior to read to prepare the WSABUF array. Where necessary,
         * it substitutes non-direct buffers with direct buffers.
         */
        void prepareBuffers() {
            shadow = new ByteBuffer[numBufs];
            long address = readBufferArray;
            for (int i=0; i<numBufs; i++) {
                ByteBuffer dst = bufs[i];
                int pos = dst.position();
                int lim = dst.limit();
                assert (pos <= lim);
                int rem = (pos <= lim ? lim - pos : 0);
                long a;
                if (!(dst instanceof DirectBuffer)) {
                    // substitute with direct buffer
                    ByteBuffer bb = Util.getTemporaryDirectBuffer(rem);
                    shadow[i] = bb;
                    a = ((DirectBuffer)bb).address();
                } else {
                    shadow[i] = dst;
                    a = ((DirectBuffer)dst).address() + pos;
                }
                unsafe.putAddress(address + OFFSETOF_BUF, a);
                unsafe.putInt(address + OFFSETOF_LEN, rem);
                address += SIZEOF_WSABUF;
            }
        }

        /**
         * Invoked after a read has completed to update the buffer positions
         * and release any substituted buffers.
         */
        void updateBuffers(int bytesRead) {
            for (int i=0; i<numBufs; i++) {
                ByteBuffer nextBuffer = shadow[i];
                int pos = nextBuffer.position();
                int len = nextBuffer.remaining();
                if (bytesRead >= len) {
                    bytesRead -= len;
                    int newPosition = pos + len;
                    try {
                        nextBuffer.position(newPosition);
                    } catch (IllegalArgumentException x) {
                        // position changed by another
                    }
                } else { // Buffers not completely filled
                    if (bytesRead > 0) {
                        assert(pos + bytesRead < (long)Integer.MAX_VALUE);
                        int newPosition = pos + bytesRead;
                        try {
                            nextBuffer.position(newPosition);
                        } catch (IllegalArgumentException x) {
                            // position changed by another
                        }
                    }
                    break;
                }
            }

            // Put results from shadow into the slow buffers
            for (int i=0; i<numBufs; i++) {
                if (!(bufs[i] instanceof DirectBuffer)) {
                    shadow[i].flip();
                    try {
                        bufs[i].put(shadow[i]);
                    } catch (BufferOverflowException x) {
                        // position changed by another
                    }
                }
            }
        }

        void releaseBuffers() {
            for (int i=0; i<numBufs; i++) {
                if (!(bufs[i] instanceof DirectBuffer)) {
                    Util.releaseTemporaryDirectBuffer(shadow[i]);
                }
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public void run() {
            long overlapped = 0L;
            boolean prepared = false;
            boolean pending = false;

            try {
                begin();

                // substitute non-direct buffers
                prepareBuffers();
                prepared = true;

                // get an OVERLAPPED structure (from the cache or allocate)
                overlapped = ioCache.add(result);

                // synchronize on result to allow this thread handle the case
                // where the read completes immediately.
                synchronized (result) {
                    int n = read0(handle, numBufs, readBufferArray, overlapped);
                    if (n == IOStatus.UNAVAILABLE) {
                        // I/O is pending
                        pending = true;
                        return;
                    }
                    // read completed immediately:
                    // 1. update buffer position
                    // 2. reset read flag
                    // 3. release waiters
                    if (n == 0) {
                        n = -1;
                    } else {
                        updateBuffers(n);
                    }
                    enableReading();

                    if (scatteringRead) {
                        result.setResult((V)Long.valueOf(n));
                    } else {
                        result.setResult((V)Integer.valueOf(n));
                    }
                }
            } catch (Throwable x) {
                // failed to initiate read:
                // 1. reset read flag
                // 2. free resources
                // 3. release waiters
                enableReading();
                if (overlapped != 0L)
                    ioCache.remove(overlapped);
                if (x instanceof ClosedChannelException)
                    x = new AsynchronousCloseException();
                if (!(x instanceof IOException))
                    x = new IOException(x);
                result.setFailure(x);
            } finally {
                if (prepared && !pending) {
                    // return direct buffer(s) to cache if substituted
                    releaseBuffers();
                }
                end();
            }

            // invoke completion handler
            Invoker.invoke(result.handler(), result);
        }

        /**
         * Executed when the I/O has completed
         */
        @Override
        @SuppressWarnings("unchecked")
        public void completed(int bytesTransferred) {
            if (bytesTransferred == 0) {
                bytesTransferred = -1;  // EOF
            } else {
                updateBuffers(bytesTransferred);
            }

            // return direct buffer to cache if substituted
            releaseBuffers();

            // release waiters if not already released by timeout
            synchronized (result) {
                if (result.isDone())
                    return;
                enableReading();
                if (scatteringRead) {
                    result.setResult((V)Long.valueOf(bytesTransferred));
                } else {
                    result.setResult((V)Integer.valueOf(bytesTransferred));
                }
            }
            Invoker.invoke(result.handler(), result);
        }

        @Override
        public void failed(int error, IOException x) {
            // return direct buffer to cache if substituted
            releaseBuffers();

            // release waiters if not already released by timeout
            if (!isOpen())
                x = new AsynchronousCloseException();

            synchronized (result) {
                if (result.isDone())
                    return;
                enableReading();
                result.setFailure(x);
            }
            Invoker.invoke(result.handler(), result);
        }

        /**
         * Invoked if timeout expires before it is cancelled
         */
        void timeout() {
            // synchronize on result as the I/O could complete/fail
            synchronized (result) {
                if (result.isDone())
                    return;

                // kill further reading before releasing waiters
                enableReading(true);
                result.setFailure(new InterruptedByTimeoutException());
            }

            // invoke handler without any locks
            Invoker.invoke(result.handler(), result);
        }
    }

    @Override
    <V extends Number,A> Future<V> readImpl(ByteBuffer[] bufs,
                                            boolean scatteringRead,
                                            long timeout,
                                            TimeUnit unit,
                                            A attachment,
                                            CompletionHandler<V,? super A> handler)
    {
        // setup task
        PendingFuture<V,A> result =
            new PendingFuture<V,A>(this, handler, attachment);
        final ReadTask readTask = new ReadTask<V,A>(bufs, scatteringRead, result);
        result.setContext(readTask);

        // schedule timeout
        if (timeout > 0L) {
            Future<?> timeoutTask = iocp.schedule(new Runnable() {
                public void run() {
                    readTask.timeout();
                }
            }, timeout, unit);
            result.setTimeoutTask(timeoutTask);
        }

        // initiate I/O (can only be done from thread in thread pool)
        Invoker.invokeOnThreadInThreadPool(this, readTask);
        return result;
    }

    /**
     * Implements the task to initiate a write and the handler to consume the
     * result when the write completes.
     */
    private class WriteTask<V,A> implements Runnable, Iocp.ResultHandler {
        private final ByteBuffer[] bufs;
        private final int numBufs;
        private final boolean gatheringWrite;
        private final PendingFuture<V,A> result;

        // set by run method
        private ByteBuffer[] shadow;

        WriteTask(ByteBuffer[] bufs,
                  boolean gatheringWrite,
                  PendingFuture<V,A> result)
        {
            this.bufs = bufs;
            this.numBufs = (bufs.length > MAX_WSABUF) ? MAX_WSABUF : bufs.length;
            this.gatheringWrite = gatheringWrite;
            this.result = result;
        }

        /**
         * Invoked prior to write to prepare the WSABUF array. Where necessary,
         * it substitutes non-direct buffers with direct buffers.
         */
        void prepareBuffers() {
            shadow = new ByteBuffer[numBufs];
            long address = writeBufferArray;
            for (int i=0; i<numBufs; i++) {
                ByteBuffer src = bufs[i];
                int pos = src.position();
                int lim = src.limit();
                assert (pos <= lim);
                int rem = (pos <= lim ? lim - pos : 0);
                long a;
                if (!(src instanceof DirectBuffer)) {
                    // substitute with direct buffer
                    ByteBuffer bb = Util.getTemporaryDirectBuffer(rem);
                    bb.put(src);
                    bb.flip();
                    src.position(pos);  // leave heap buffer untouched for now
                    shadow[i] = bb;
                    a = ((DirectBuffer)bb).address();
                } else {
                    shadow[i] = src;
                    a = ((DirectBuffer)src).address() + pos;
                }
                unsafe.putAddress(address + OFFSETOF_BUF, a);
                unsafe.putInt(address + OFFSETOF_LEN, rem);
                address += SIZEOF_WSABUF;
            }
        }

        /**
         * Invoked after a write has completed to update the buffer positions
         * and release any substituted buffers.
         */
        void updateBuffers(int bytesWritten) {
            // Notify the buffers how many bytes were taken
            for (int i=0; i<numBufs; i++) {
                ByteBuffer nextBuffer = bufs[i];
                int pos = nextBuffer.position();
                int lim = nextBuffer.limit();
                int len = (pos <= lim ? lim - pos : lim);
                if (bytesWritten >= len) {
                    bytesWritten -= len;
                    int newPosition = pos + len;
                    try {
                        nextBuffer.position(newPosition);
                    } catch (IllegalArgumentException x) {
                        // position changed by someone else
                    }
                } else { // Buffers not completely filled
                    if (bytesWritten > 0) {
                        assert(pos + bytesWritten < (long)Integer.MAX_VALUE);
                        int newPosition = pos + bytesWritten;
                        try {
                            nextBuffer.position(newPosition);
                        } catch (IllegalArgumentException x) {
                            // position changed by someone else
                        }
                    }
                    break;
                }
            }
        }

        void releaseBuffers() {
            for (int i=0; i<numBufs; i++) {
                if (!(bufs[i] instanceof DirectBuffer)) {
                    Util.releaseTemporaryDirectBuffer(shadow[i]);
                }
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public void run() {
            int n = -1;
            long overlapped = 0L;
            boolean prepared = false;
            boolean pending = false;
            boolean shutdown = false;

            try {
                begin();

                // substitute non-direct buffers
                prepareBuffers();
                prepared = true;

                // get an OVERLAPPED structure (from the cache or allocate)
                overlapped = ioCache.add(result);

                // synchronize on result to allow this thread handle the case
                // where the read completes immediately.
                synchronized (result) {
                    n = write0(handle, numBufs, writeBufferArray, overlapped);
                    if (n == IOStatus.UNAVAILABLE) {
                        // I/O is pending
                        pending = true;
                        return;
                    }

                    enableWriting();

                    if (n == IOStatus.EOF) {
                        // special case for shutdown output
                        shutdown = true;
                        throw new ClosedChannelException();
                    }

                    // write completed immediately:
                    // 1. enable writing
                    // 2. update buffer position
                    // 3. release waiters
                    updateBuffers(n);

                    // result is a Long or Integer
                    if (gatheringWrite) {
                        result.setResult((V)Long.valueOf(n));
                    } else {
                        result.setResult((V)Integer.valueOf(n));
                    }
                }
            } catch (Throwable x) {
                enableWriting();

                // failed to initiate read:
                if (!shutdown && (x instanceof ClosedChannelException))
                    x = new AsynchronousCloseException();
                if (!(x instanceof IOException))
                    x = new IOException(x);
                result.setFailure(x);

                // release resources
                if (overlapped != 0L)
                    ioCache.remove(overlapped);

            } finally {
                if (prepared && !pending) {
                    // return direct buffer(s) to cache if substituted
                    releaseBuffers();
                }
                end();
            }

            // invoke completion handler
            Invoker.invoke(result.handler(), result);
        }

        /**
         * Executed when the I/O has completed
         */
        @Override
        @SuppressWarnings("unchecked")
        public void completed(int bytesTransferred) {
            updateBuffers(bytesTransferred);

            // return direct buffer to cache if substituted
            releaseBuffers();

            // release waiters if not already released by timeout
            synchronized (result) {
                if (result.isDone())
                    return;
                enableWriting();
                if (gatheringWrite) {
                    result.setResult((V)Long.valueOf(bytesTransferred));
                } else {
                    result.setResult((V)Integer.valueOf(bytesTransferred));
                }
            }
            Invoker.invoke(result.handler(), result);
        }

        @Override
        public void failed(int error, IOException x) {
            // return direct buffer to cache if substituted
            releaseBuffers();

            // release waiters if not already released by timeout
            if (!isOpen())
                x = new AsynchronousCloseException();

            synchronized (result) {
                if (result.isDone())
                    return;
                enableWriting();
                result.setFailure(x);
            }
            Invoker.invoke(result.handler(), result);
        }

        /**
         * Invoked if timeout expires before it is cancelled
         */
        void timeout() {
            // synchronize on result as the I/O could complete/fail
            synchronized (result) {
                if (result.isDone())
                    return;

                // kill further writing before releasing waiters
                enableWriting(true);
                result.setFailure(new InterruptedByTimeoutException());
            }

            // invoke handler without any locks
            Invoker.invoke(result.handler(), result);
        }
    }

    @Override
    <V extends Number,A> Future<V> writeImpl(ByteBuffer[] bufs,
                                             boolean gatheringWrite,
                                             long timeout,
                                             TimeUnit unit,
                                             A attachment,
                                             CompletionHandler<V,? super A> handler)
    {
        // setup task
        PendingFuture<V,A> result =
            new PendingFuture<V,A>(this, handler, attachment);
        final WriteTask writeTask = new WriteTask<V,A>(bufs, gatheringWrite, result);
        result.setContext(writeTask);

        // schedule timeout
        if (timeout > 0L) {
            Future<?> timeoutTask = iocp.schedule(new Runnable() {
                public void run() {
                    writeTask.timeout();
                }
            }, timeout, unit);
            result.setTimeoutTask(timeoutTask);
        }

        // initiate I/O (can only be done from thread in thread pool)
        Invoker.invokeOnThreadInThreadPool(this, writeTask);
        return result;
    }

    // -- Native methods --

    private static native void initIDs();

    private static native int connect0(long socket, boolean preferIPv6,
        InetAddress remote, int remotePort, long overlapped) throws IOException;

    private static native void updateConnectContext(long socket) throws IOException;

    private static native int read0(long socket, int count, long addres, long overlapped)
        throws IOException;

    private static native int write0(long socket, int count, long address,
        long overlapped) throws IOException;

    private static native void shutdown0(long socket, int how) throws IOException;

    private static native void closesocket0(long socket) throws IOException;

    static {
        Util.load();
        initIDs();
    }
}
