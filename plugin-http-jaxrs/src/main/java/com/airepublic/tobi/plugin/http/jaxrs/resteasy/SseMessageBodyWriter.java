package com.airepublic.tobi.plugin.http.jaxrs.resteasy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseEvent;

/**
 * A {@link MessageBodyWriter} for {@link OutboundSseEvent}.
 * 
 * @author Torsten Oltmanns
 *
 */
public class SseMessageBodyWriter implements MessageBodyWriter<OutboundSseEvent> {

    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
        return OutboundSseEvent.class.isAssignableFrom(type);
    }


    @Override
    public void writeTo(final OutboundSseEvent event, final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders, final OutputStream entityStream) throws IOException, WebApplicationException {
        final StringBuffer buffer = new StringBuffer();

        if (event.getName() != null) {
            buffer.append("event:").append(event.getName()).append("\n");
        }

        if (event.getId() != null) {
            buffer.append("id:").append(event.getId()).append("\n");
        }

        if (event.getComment() != null) {
            final BufferedReader reader = new BufferedReader(new StringReader(event.getComment()));
            String line = reader.readLine();

            while (line != null) {
                buffer.append(":").append(line).append("\n");
                line = reader.readLine();
            }
        }

        if (event.getReconnectDelay() != SseEvent.RECONNECT_NOT_SET) {
            buffer.append("retry:").append(event.getReconnectDelay()).append("\n");
        }

        final BufferedReader reader = new BufferedReader(new StringReader(event.getData().toString()));
        String line = reader.readLine();

        while (line != null) {
            buffer.append("data:").append(line).append("\n");
            line = reader.readLine();
        }

        buffer.append("\n");

    }

}
