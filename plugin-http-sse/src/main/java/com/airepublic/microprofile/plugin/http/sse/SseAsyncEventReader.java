package com.airepublic.microprofile.plugin.http.sse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import com.airepublic.microprofile.util.http.common.BufferUtil;

public class SseAsyncEventReader {
    private InboundSseEventImpl currentEvent = new InboundSseEventImpl();
    private final Queue<InboundSseEventImpl> events = new ConcurrentLinkedDeque<>();
    private boolean isReadName = true;
    private String currentName = "";
    private String currentValue = "";


    public boolean receiveBuffer(final ByteBuffer buffer) throws IOException {
        String line = "";
        boolean addedEvent = false;

        // read all blank lines
        while (line.isBlank()) {
            line = BufferUtil.readLine(buffer, Charset.forName("UTF-8"));
        }

        // first line contains the content-length in hexadecimal
        final int contentLength = Integer.parseInt(line.strip(), 16);

        if (contentLength > 0) {
            final byte[] bytes = new byte[contentLength];
            buffer.get(bytes);

            final String eventMessage = new String(bytes, Charset.forName("UTF-8"));
            final BufferedReader reader = new BufferedReader(new StringReader(eventMessage));

            line = reader.readLine();

            while (line != null) {

                if (line.isBlank() && currentEvent.readData() != null && currentEvent.readData().length() > 0 && !events.contains(currentEvent)) {
                    events.add(currentEvent);
                    addedEvent = true;
                    currentEvent = new InboundSseEventImpl();
                }

                int pos = 0;

                if (isReadName) {
                    int idx = line.indexOf(':');

                    if (idx == -1) {
                        currentName = currentName + line;
                        pos = line.length();
                    } else {
                        currentName = line.substring(0, idx);
                        pos = ++idx;

                        isReadName = false;
                    }
                }

                if (!isReadName) {
                    currentValue = currentValue + line.substring(pos);
                    pos = line.length();

                    if (currentValue.length() > 0) {
                        isReadName = true;
                    }
                }

                if (currentValue.length() > 0) {
                    if (currentName.equalsIgnoreCase("event")) {
                        currentEvent.setName(currentValue);
                    } else if (currentName.equalsIgnoreCase("id")) {
                        currentEvent.setId(currentValue);
                    } else if (currentName.equalsIgnoreCase("retry")) {
                        currentEvent.setReconnectDelay(Long.valueOf(currentValue));
                    } else if (currentName.equalsIgnoreCase("data")) {
                        currentEvent.setData((currentEvent.readData() != null ? currentEvent.readData() : "") + currentValue);
                    } else if (currentName.isBlank()) {
                        currentEvent.setComment(currentValue);
                    }
                }

                line = reader.readLine();

            }

            // check if the buffer has only one event without separator (\n\n)
            if (currentEvent.readData() != null && currentEvent.readData().length() > 0 && !events.contains(currentEvent)) {
                events.add(currentEvent);
                addedEvent = true;
                currentEvent = new InboundSseEventImpl();
            }
        }

        return addedEvent;
    }


    public InboundSseEventImpl poll() {
        return events.isEmpty() ? null : events.poll();
    }
}
