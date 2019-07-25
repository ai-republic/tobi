package com.airepublic.microprofile.util.http.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Objects;

/**
 * Utility methods to process a {@link ByteBuffer}.
 * 
 * @author Torsten Oltmanns
 *
 */
public class BufferUtil {
    public static String readLine(final ByteBuffer buffer, final Charset charset) throws IOException {
        byte cur = ' ';

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            while (buffer.hasRemaining()) {
                cur = buffer.get();

                bos.write(cur);

                if (cur == (byte) '\n') {
                    return new String(bos.toByteArray(), charset);
                }
            }
        } catch (final UnsupportedEncodingException e) {
        }

        return null;
    }


    public static String readNextToken(final ByteBuffer buffer, final String token, final Charset charset) throws IOException {
        Objects.nonNull(buffer);
        Objects.nonNull(token);

        byte cur;
        String str = null;

        buffer.mark();

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            while (buffer.hasRemaining()) {
                cur = buffer.get();
                bos.write(cur);

                str = new String(bos.toByteArray(), charset);

                if (str.endsWith(token)) {
                    return str.substring(0, str.length() - token.length());
                }
            }
        } catch (final UnsupportedEncodingException e) {
        }

        return null;
    }


    public static ByteBuffer combineBuffers(final Collection<ByteBuffer> buffers) throws IOException {

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            for (final ByteBuffer buffer : buffers) {
                final byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                bos.write(bytes);
            }

            return ByteBuffer.wrap(bos.toByteArray());
        } catch (final IOException e) {
            throw e;
        }
    }


    public static ByteBuffer copyRemainingBuffer(final ByteBuffer buffer) {
        final byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return ByteBuffer.wrap(bytes);
    }
}
