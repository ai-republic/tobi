package com.airepublic.microprofile.sample;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;

import com.airepublic.microprofile.plugin.http.sse.SseRepeat;
import com.airepublic.microprofile.plugin.http.sse.SseUri;

@Path("/sse")
public class SseSample {
    @Inject
    private SseEventSink sink;
    @Inject
    private Sse sse;
    private int counter = 0;


    @Produces("text/event-stream")
    @Path("/produce")
    @SseRepeat(delay = 1, unit = TimeUnit.SECONDS, maxTimes = 5)
    public void produce() {

        final String[] words = new String[] { "Hello", "World", "from", "the", "SSE" };
        final OutboundSseEvent event = sse.newEventBuilder().name("MyEvent").reconnectDelay(1000L).data(words[counter % 5]).build();
        counter++;

        sink.send(event);
        // sink.close();
    }


    @Consumes("text/event-stream")
    @SseUri("https://api.boerse-frankfurt.de/data/frankfurt_trading_parameter?isin=US00724F1012")
    public void consumes(final InboundSseEvent event) {
        System.out.println(event.readData());
    }

}
