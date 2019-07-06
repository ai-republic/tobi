package com.airepublic.microprofile.util.http.common;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class HttpBufferUtils {

    public static String getUriPath(final ByteBuffer buffer) throws IOException {
        String path = null;

        // mark buffer to reset it after read to leave it untouched for handler
        final String line = BufferUtil.readLine(buffer, Charset.forName("ASCII"));

        if (line != null && !line.contains(" HTTP")) {
            throw new IOException(line + " does not contain valid URI");
        }

        // check for the URI request line
        if (line != null) {
            final int startIdx = line.indexOf(" ");

            if (startIdx != -1) {
                final int endIdx = line.indexOf(" ", startIdx + 1);

                if (endIdx != -1) {
                    path = line.substring(startIdx, endIdx).strip();

                    // strip trailing slash if there is one
                    if (path != null && path.endsWith("/")) {
                        path = path.substring(0, path.length() - 1);
                    }
                } else {
                    throw new IOException(line + " does not contain valid URI");
                }
            } else {
                throw new IOException(line + " does not contain valid URI");
            }

            return path;
        }

        return null;
    }

}
