
package com.airepublic.tobi.example;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

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
    private final int counter = 0;


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

}