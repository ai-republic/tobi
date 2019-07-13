
package com.airepublic.microprofile.sample;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

@Path("/service")
public final class Service {

    @GET
    public String getStuff(@QueryParam("hello") final String hello) {
        return "get " + hello;
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