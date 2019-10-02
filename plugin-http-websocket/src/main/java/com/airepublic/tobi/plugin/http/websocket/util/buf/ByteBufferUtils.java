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
package com.airepublic.tobi.plugin.http.websocket.util.buf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.airepublic.logging.java.SerializableLogger;
import com.airepublic.tobi.plugin.http.websocket.util.res.StringManager;

public class ByteBufferUtils {

    private static final StringManager sm = StringManager.getManager(ByteBufferUtils.class);
    private static final Logger log = new SerializableLogger(ByteBufferUtils.class.getName());

    private static final Object unsafe;
    private static final Method cleanerMethod;
    private static final Method cleanMethod;
    private static final Method invokeCleanerMethod;

    static {
        final ByteBuffer tempBuffer = ByteBuffer.allocateDirect(0);
        Method cleanerMethodLocal = null;
        Method cleanMethodLocal = null;
        final Object unsafeLocal = null;
        final Method invokeCleanerMethodLocal = null;

        try {
            cleanerMethodLocal = tempBuffer.getClass().getMethod("cleaner");
            cleanerMethodLocal.setAccessible(true);
            final Object cleanerObject = cleanerMethodLocal.invoke(tempBuffer);
            cleanMethodLocal = cleanerObject.getClass().getMethod("clean");
            cleanMethodLocal.invoke(cleanerObject);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            log.log(Level.WARNING, sm.getString("byteBufferUtils.cleaner"), e);
            cleanerMethodLocal = null;
            cleanMethodLocal = null;
        }

        cleanerMethod = cleanerMethodLocal;
        cleanMethod = cleanMethodLocal;
        unsafe = unsafeLocal;
        invokeCleanerMethod = invokeCleanerMethodLocal;
    }


    private ByteBufferUtils() {
        // Hide the default constructor since this is a utility class.
    }


    public static String readLine(final ByteBuffer buf) throws IOException {
        byte prev;
        byte cur = ' ';

        buf.mark();

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            while (buf.hasRemaining()) {
                prev = cur;
                cur = buf.get();
                bos.write(cur);

                if (prev == (byte) '\r' && cur == (byte) '\n') {
                    return new String(bos.toByteArray(), "ASCII");
                }
            }
        } catch (final UnsupportedEncodingException e) {
        }

        buf.reset();

        return null;
    }


    /**
     * Expands buffer to the given size unless it is already as big or bigger. Buffers are assumed
     * to be in 'write to' mode since there would be no need to expand a buffer while it was in
     * 'read from' mode.
     *
     * @param in Buffer to expand
     * @param newSize The size t which the buffer should be expanded
     * @return The expanded buffer with any data from the input buffer copied in to it or the
     *         original buffer if there was no need for expansion
     */
    public static ByteBuffer expand(final ByteBuffer in, final int newSize) {
        if (in.capacity() >= newSize) {
            return in;
        }

        ByteBuffer out;
        boolean direct = false;
        if (in.isDirect()) {
            out = ByteBuffer.allocateDirect(newSize);
            direct = true;
        } else {
            out = ByteBuffer.allocate(newSize);
        }

        // Copy data
        in.flip();
        out.put(in);

        if (direct) {
            cleanDirectBuffer(in);
        }

        return out;
    }


    public static void cleanDirectBuffer(final ByteBuffer buf) {
        if (cleanMethod != null) {
            try {
                cleanMethod.invoke(cleanerMethod.invoke(buf));
            } catch (IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | SecurityException e) {
                // Ignore
            }
        } else if (invokeCleanerMethod != null) {
            try {
                invokeCleanerMethod.invoke(unsafe, buf);
            } catch (IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | SecurityException e) {
                // Ignore
                e.printStackTrace();
            }
        }
    }

}
