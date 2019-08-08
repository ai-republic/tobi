
package com.airepublic.tobi.example;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;

import com.airepublic.tobi.core.spi.IServerSession;

/**
 * Example JAX-RS resource.
 * 
 * @author Torsten Oltmanns
 *
 */
@Path("/service")
public class Service {
    @Inject
    private IServerSession session;
    private SseBroadcaster broadcaster;
    @Inject
    private Sse sse;
    private final int broadcastCounter = 0;
    private int counter = 0;


    /**
     * Initializes the {@link SseBroadcaster}.
     */
    @PostConstruct
    public void init() {
        broadcaster = sse.newBroadcaster();
    }


    @GET
    public String getStuff(@QueryParam("hello") final String hello) {
        return "get " + hello + " session#" + session.getId();
    }


    @POST
    public String postStuff(final String body) {
        System.out.println(body);
        return "post";
    }


    @Path("contract")
    @GET
    public String contractStuff() {
        return "contract";
    }


    @Path("register")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void register(@Context final SseEventSink sseEventSink) {
        broadcaster.register(sseEventSink);
        sseEventSink.send(sse.newEvent("Registered"));
    }


    @Path("broadcast")
    public void broadcast() throws InterruptedException {
        final String[] words = new String[] { "Hello", "World", "from", "the", "SSE", "Broadcaster" };

        for (int i = 0; i < 6; i++) {
            final OutboundSseEvent event = sse.newEventBuilder().name("MyEvent").reconnectDelay(1000L).data(words[broadcastCounter % 5]).build();
            broadcaster.broadcast(event);
            Thread.sleep(1000);
        }
    }


    @Produces(MediaType.SERVER_SENT_EVENTS)
    @Path("/produce")
    // @SseProducer(delay = 1, unit = TimeUnit.SECONDS, maxTimes = 5)
    public void produce(@Context final SseEventSink sink) {

        final String[] words = new String[] { "Hello", "World", "from", "the", "SSE" };
        final OutboundSseEvent event = sse.newEventBuilder().name("MyEvent").reconnectDelay(1000L).data(words[counter % 5]).build();
        counter++;

        sink.send(event);
        // sink.close();
    }
}