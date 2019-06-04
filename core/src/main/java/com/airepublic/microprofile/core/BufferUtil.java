package com.airepublic.microprofile.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class BufferUtil {
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
}
