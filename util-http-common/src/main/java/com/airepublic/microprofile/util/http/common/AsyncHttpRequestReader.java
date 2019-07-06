package com.airepublic.microprofile.util.http.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class AsyncHttpRequestReader {
    private final List<ByteBuffer> requestBuffers = new ArrayList<>();
    private boolean requestFullyRead = false;
    private HttpRequest httpRequest;


    /**
     * This method will parse the input buffer. If the request is fully read it will return true
     * otherwise false if more data is needed.
     * 
     * @param buffer
     * @return if the request is fully read it will return true otherwise false if more data is
     *         needed
     * @throws IOException if reading from the buffer fails
     */
    public boolean receiveRequestBuffer(final ByteBuffer buffer) throws IOException {
        if (!requestFullyRead) {
            if (buffer.hasRemaining()) {
                final byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                requestBuffers.add(ByteBuffer.wrap(bytes));
            }

            // check if finished receiving incoming buffers
            if (buffer.limit() == buffer.capacity()) {
                requestFullyRead = false;
            } else {
                requestFullyRead = true;
            }
        } else {
            throw new IOException("Request has already been fully read. Illegal incoming second request!");
        }

        return requestFullyRead;
    }


    public boolean isRequestFullyRead() {
        return requestFullyRead;
    }


    public HttpRequest getHttpRequest() throws IOException {
        if (httpRequest == null) {
            if (requestFullyRead) {
                httpRequest = parseBuffers();
            } else {
                throw new IOException("Illegal state - request has not been fully read!");
            }
        }

        return httpRequest;
    }


    private HttpRequest parseBuffers() throws IOException {
        if (httpRequest == null) {
            final HttpRequest request = new HttpRequest();
            boolean isFinishedHeaders = false;
            boolean isFirstLine = true;
            final ByteArrayOutputStream bis = new ByteArrayOutputStream();

            for (final ByteBuffer buffer : requestBuffers) {
                if (!isFinishedHeaders) {
                    while (buffer.hasRemaining()) {

                        String line = BufferUtil.readLine(buffer, Charset.forName("ASCII"));

                        if (line != null) {
                            line = line.strip();
                            System.out.println(line);

                            // check if line is empty
                            if (line.isBlank()) {
                                isFinishedHeaders = true;
                                // read all following blank lines
                                while (buffer.hasRemaining() && line != null && line.isBlank()) {
                                    line = BufferUtil.readLine(buffer, Charset.forName("ASCII"));
                                }
                                continue;
                            } else {
                                if (isFirstLine) {
                                    request.setRequestLine(line);
                                    isFirstLine = false;
                                } else {
                                    final int idx = line.indexOf(':');

                                    if (idx != -1) {
                                        final String value = line.substring(idx + 1);
                                        request.getHeaders().add(line.substring(0, idx), value.strip());
                                    } else if (idx == -1) {
                                        throw new IOException("Could not parse header information: " + line);
                                    }
                                }
                            }
                        } else {
                            throw new IOException("Could not read header line (null)!");
                        }
                    }
                }

                if (buffer.hasRemaining()) {
                    // read body
                    final byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);
                    bis.write(bytes);
                }
            }

            request.withBody(ByteBuffer.wrap(bis.toByteArray()));
            bis.close();

            httpRequest = request;
        }

        return httpRequest;
    }
}
