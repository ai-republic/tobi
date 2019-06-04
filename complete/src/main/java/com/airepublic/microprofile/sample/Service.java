
package com.airepublic.microprofile.sample;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/service")
public final class Service {

    @GET
    public String getStuff() {
        return "get";
    }


    @POST
    public String postStuff() {
        return "post";
    }


    @Path("contract")
    @GET
    public String contractStuff() {
        return "contract";
    }
}