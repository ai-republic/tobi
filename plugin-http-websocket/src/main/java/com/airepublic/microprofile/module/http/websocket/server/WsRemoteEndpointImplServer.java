/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.airepublic.microprofile.module.http.websocket.server;

import java.io.EOFException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;

import javax.websocket.SendHandler;
import javax.websocket.SendResult;

import com.airepublic.microprofile.module.http.websocket.Transformation;
import com.airepublic.microprofile.module.http.websocket.WsRemoteEndpointImplBase;

/**
 * This is the server side {@link javax.websocket.RemoteEndpoint} implementation - i.e. what the
 * server uses to send data to the client.
 */
public abstract class WsRemoteEndpointImplServer extends WsRemoteEndpointImplBase {
    protected final WsWriteTimeout wsWriteTimeout;
    private volatile SendHandler handler = null;

    private volatile long timeoutExpiry = -1;
    private volatile boolean close;


    public WsRemoteEndpointImplServer(final WsServerContainer serverContainer) {
        wsWriteTimeout = serverContainer.getTimeout();
    }


    @Override
    protected final boolean isMasked() {
        return false;
    }


    @Override
    protected void doWrite(final SendHandler handler, final long blockingWriteTimeoutExpiry, final ByteBuffer... buffers) {

        final boolean block = blockingWriteTimeoutExpiry != -1;
        long timeout = -1;

        if (block) {
            timeout = blockingWriteTimeoutExpiry - System.currentTimeMillis();

            if (timeout <= 0) {
                final SendResult sr = new SendResult(new SocketTimeoutException());
                handler.onResult(sr);
                return;
            }
        } else {
            this.handler = handler;

            if (timeout > 0) {
                // Register with timeout thread
                timeoutExpiry = timeout + System.currentTimeMillis();
                wsWriteTimeout.register(this);
            }

            timeout = getSendTimeout();
        }

        write(block, timeout, TimeUnit.MILLISECONDS, new CompletionHandler<Long, Void>() {
            @Override
            public void completed(final Long result, final Void attachment) {
                if (block) {
                    final long timeout = blockingWriteTimeoutExpiry - System.currentTimeMillis();

                    if (timeout <= 0) {
                        failed(new SocketTimeoutException(), null);
                    } else {
                        handler.onResult(SENDRESULT_OK);
                    }
                } else {
                    wsWriteTimeout.unregister(WsRemoteEndpointImplServer.this);
                    clearHandler(null, true);

                    if (close) {
                        close();
                    }
                }
            }


            @Override
            public void failed(final Throwable exc, final Void attachment) {
                if (block) {
                    final SendResult sr = new SendResult(exc);
                    handler.onResult(sr);
                } else {
                    wsWriteTimeout.unregister(WsRemoteEndpointImplServer.this);
                    clearHandler(exc, true);
                    close();
                }
            }
        }, buffers);
    }


    protected abstract void write(boolean block, long timeout, TimeUnit unit, CompletionHandler<?, ?> handler, ByteBuffer... buffers);


    @Override
    protected void doClose() {
        if (handler != null) {
            // close() can be triggered by a wide range of scenarios. It is far
            // simpler just to always use a dispatch than it is to try and track
            // whether or not this method was called by the same thread that
            // triggered the write
            clearHandler(new EOFException(), true);
        }
        wsWriteTimeout.unregister(this);
    }


    protected long getTimeoutExpiry() {
        return timeoutExpiry;
    }


    /*
     * Currently this is only called from the background thread so we could just call clearHandler()
     * with useDispatch == false but the method parameter was added in case other callers started to
     * use this method to make sure that those callers think through what the correct value of
     * useDispatch is for them.
     */
    protected void onTimeout(final boolean useDispatch) {
        if (handler != null) {
            clearHandler(new SocketTimeoutException(), useDispatch);
        }
        close();
    }


    @Override
    public void setTransformation(final Transformation transformation) {
        // Overridden purely so it is visible to other classes in this package
        super.setTransformation(transformation);
    }


    /**
     *
     * @param t The throwable associated with any error that occurred
     * @param useDispatch Should {@link SendHandler#onResult(SendResult)} be called from a new
     *        thread, keeping in mind the requirements of
     *        {@link javax.websocket.RemoteEndpoint.Async}
     */
    protected void clearHandler(final Throwable t, final boolean useDispatch) {
        // Setting the result marks this (partial) message as
        // complete which means the next one may be sent which
        // could update the value of the handler. Therefore, keep a
        // local copy before signalling the end of the (partial)
        // message.
        final SendHandler sh = handler;
        handler = null;

        if (sh != null) {
            if (useDispatch) {
                final OnResultRunnable r = new OnResultRunnable(sh, t);

                r.run();

            } else {
                if (t == null) {
                    sh.onResult(new SendResult());
                } else {
                    sh.onResult(new SendResult(t));
                }
            }
        }
    }

    private static class OnResultRunnable implements Runnable {

        private final SendHandler sh;
        private final Throwable t;


        private OnResultRunnable(final SendHandler sh, final Throwable t) {
            this.sh = sh;
            this.t = t;
        }


        @Override
        public void run() {
            if (t == null) {
                sh.onResult(new SendResult());
            } else {
                sh.onResult(new SendResult(t));
            }
        }
    }
}
