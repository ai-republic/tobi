/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.airepublic.tobi.plugin.http.websocket.server;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.airepublic.logging.java.SerializableLogger;
import com.airepublic.tobi.plugin.http.websocket.Transformation;
import com.airepublic.tobi.plugin.http.websocket.WsFrameBase;
import com.airepublic.tobi.plugin.http.websocket.WsIOException;
import com.airepublic.tobi.plugin.http.websocket.WsSession;
import com.airepublic.tobi.plugin.http.websocket.util.res.StringManager;

public class WsFrameServer extends WsFrameBase {

    private final Logger log = new SerializableLogger(WsFrameServer.class.getName()); // must not be
    // static
    private static final StringManager sm = StringManager.getManager(WsFrameServer.class);

    private final ClassLoader applicationClassLoader;


    public WsFrameServer(final WsSession wsSession, final Transformation transformation, final ClassLoader applicationClassLoader) {
        super(wsSession, transformation);
        this.applicationClassLoader = applicationClassLoader;
    }


    /**
     * Called when there is data in the ServletInputStream to process.
     *
     * @param buffer the received {@link ByteBuffer}
     * @throws IOException if an I/O error occurs while processing the available data
     */
    public void onDataAvailable(final ByteBuffer buffer) throws IOException {
        if (log.isLoggable(Level.FINEST)) {
            log.log(Level.FINEST, "wsFrameServer.onDataAvailable");
        }
        if (isOpen() && inputBuffer.hasRemaining() && !isSuspended()) {
            // There might be a data that was left in the buffer when
            // the read has been suspended.
            // Consume this data before reading from the socket.
            processInputBuffer();
        }

        while (isOpen() && !isSuspended()) {
            // Fill up the input buffer with as much data as we can
            inputBuffer.mark();
            inputBuffer.position(inputBuffer.limit()).limit(inputBuffer.capacity());

            // TODO block write buffer
            int read = 0;

            while (buffer.hasRemaining() && inputBuffer.position() < inputBuffer.capacity()) {
                final byte value = buffer.get();
                inputBuffer.put(value);
                read++;
            }

            inputBuffer.limit(inputBuffer.position()).reset();

            if (read < 0) {
                throw new EOFException();
            } else if (read == 0) {
                return;
            }
            if (log.isLoggable(Level.FINEST)) {
                log.log(Level.FINEST, sm.getString("wsFrameServer.bytesRead", Integer.toString(read)));
            }
            processInputBuffer();
        }
    }


    @Override
    protected boolean isMasked() {
        // Data is from the client so it should be masked
        return true;
    }


    @Override
    public Transformation getTransformation() {
        // Overridden to make it visible to other classes in this package
        return super.getTransformation();
    }


    @Override
    protected boolean isOpen() {
        // Overridden to make it visible to other classes in this package
        return super.isOpen();
    }


    @Override
    protected Logger getLog() {
        return log;
    }


    @Override
    protected void sendMessageText(final boolean last) throws WsIOException {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(applicationClassLoader);
            super.sendMessageText(last);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }


    @Override
    protected void sendMessageBinary(final ByteBuffer msg, final boolean last) throws WsIOException {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(applicationClassLoader);
            super.sendMessageBinary(msg, last);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

}
