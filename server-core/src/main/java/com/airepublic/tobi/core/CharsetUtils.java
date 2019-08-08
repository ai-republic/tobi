/*
 * Copyright (c) 2010-2019 Nathan Rajlich
 *
 *  Permission is hereby granted, free of charge, to any person
 *  obtaining a copy of this software and associated documentation
 *  files (the "Software"), to deal in the Software without
 *  restriction, including without limitation the rights to use,
 *  copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following
 *  conditions:
 *
 *  The above copyright notice and this permission notice shall be
 *  included in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 *  OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 *  HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *  WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 *  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 *  OTHER DEALINGS IN THE SOFTWARE.
 */

package com.airepublic.tobi.core;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;

/**
 * Utilities to handle reading of different {@link Charset} from strings, bytes or
 * {@link ByteBuffer}s.
 * 
 * @author Torsten Oltmanns
 *
 */
public class CharsetUtils {

    /**
     * Gets the string as UTF-8 encoded bytes.
     * 
     * @param s the string
     * @return the string as UTF-8 encoded bytes
     */
    public static byte[] utf8Bytes(final String s) {
        try {
            return s.getBytes("UTF8");
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Gets the string as ASCII encoded bytes.
     * 
     * @param s the string
     * @return the string as ASCII encoded bytes
     */
    public static byte[] asciiBytes(final String s) {
        try {
            return s.getBytes("ASCII");
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Creates a string from ASCII encoded bytes.
     * 
     * @param bytes the bytes
     * @return the string
     */
    public static String stringAscii(final byte[] bytes) {
        try {
            return new String(bytes, "ASCII");
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Creates a string from UTF-8 encoded bytes.
     * 
     * @param bytes the bytes
     * @return the string
     */
    public static String stringUtf8(final byte[] bytes) {
        return stringUtf8(ByteBuffer.wrap(bytes));
    }


    /**
     * Creates a string from UTF-8 encoded {@link ByteBuffer}.
     * 
     * @param buffer the {@link ByteBuffer}
     * @return the string
     */
    public static String stringUtf8(final ByteBuffer buffer) {
        final CharsetDecoder decode = Charset.forName("UTF8").newDecoder();
        decode.onMalformedInput(CodingErrorAction.REPORT);
        decode.onUnmappableCharacter(CodingErrorAction.REPORT);

        String s;

        try {
            buffer.mark();
            s = decode.decode(buffer).toString();
            buffer.reset();
        } catch (final CharacterCodingException e) {
            throw new RuntimeException(e);
        }

        return s;
    }
}
