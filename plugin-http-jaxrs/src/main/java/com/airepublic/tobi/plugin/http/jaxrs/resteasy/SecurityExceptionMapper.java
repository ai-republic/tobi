package com.airepublic.tobi.plugin.http.jaxrs.resteasy;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Resteasy {@link SecurityException} mapper.
 * 
 * @author Torsten Oltmanns
 *
 */
@Provider
public class SecurityExceptionMapper implements ExceptionMapper<SecurityException> {

    @Override
    public Response toResponse(final SecurityException exception) {
        return Response.status(Status.UNAUTHORIZED).build();
    }
}
