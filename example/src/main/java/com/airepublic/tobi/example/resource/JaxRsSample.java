
package com.airepublic.tobi.example.resource;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.jwt.JsonWebToken;

import com.airepublic.http.common.Headers;
import com.airepublic.tobi.feature.mp.jwtauth.Authorized;

/**
 * Example JAX-RS resource.
 * 
 * @author Torsten Oltmanns
 *
 */
@Path("/service")
public class JaxRsSample {
    private final int counter = 0;
    @Inject
    private JsonWebToken jwt;


    @GET
    @Authorized(allow = "ADMIN")
    public Response getStuff(@QueryParam("hello") final String hello, @Context final HttpHeaders headers) {
        return Response.ok("get: Hello " + hello + ", headers=" + headers).header(HttpHeaders.AUTHORIZATION, jwt.getRawToken()).build();
    }


    @POST
    public String postStuff(final String body) {
        System.out.println(body);
        return "post: body";
    }


    @Path("register")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response registerStuff() {
        return Response.ok().header(Headers.AUTHORIZATION, jwt).build();
    }

}