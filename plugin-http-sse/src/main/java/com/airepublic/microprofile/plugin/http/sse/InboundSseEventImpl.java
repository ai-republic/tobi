package com.airepublic.microprofile.plugin.http.sse;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.InboundSseEvent;

public class InboundSseEventImpl extends SseEventImpl implements InboundSseEvent {
    private String data;


    @Override
    public boolean isEmpty() {
        // TODO Auto-generated method stub
        return false;
    }


    void setData(final String data) {
        this.data = data;
    }


    @Override
    public String readData() {
        return data;
    }


    @Override
    public <T> T readData(final Class<T> type) {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public <T> T readData(final GenericType<T> type) {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public <T> T readData(final Class<T> messageType, final MediaType mediaType) {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public <T> T readData(final GenericType<T> type, final MediaType mediaType) {
        // TODO Auto-generated method stub
        return null;
    }

}
