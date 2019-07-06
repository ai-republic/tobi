package com.airepublic.microprofile.plugin.http.sse;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.sse.OutboundSseEvent.Builder;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;

@ApplicationScoped
public class SseImpl implements Sse {

    @Override
    public Builder newEventBuilder() {
        return new OutboundSseEventBuilder();
    }


    @Override
    public SseBroadcaster newBroadcaster() {
        return new SseBroadcasterImpl();
    }

}
