package com.airepublic.tobi.core.spi;

import java.io.IOException;

public interface IAuthorizationProvider {
    /**
     * Provide authorization information encoded in the {@link IRequest} or {@link IServerSession}.
     * 
     * @param session the {@link IServerSession}
     * @throws IOException if something goes wrong
     */
    void accept(IServerSession session) throws IOException;
}
