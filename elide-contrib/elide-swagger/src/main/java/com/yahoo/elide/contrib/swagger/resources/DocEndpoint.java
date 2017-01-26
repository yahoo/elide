/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.swagger.resources;

import com.yahoo.elide.contrib.swagger.SwaggerBuilder;
import io.swagger.models.Swagger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * A convenience endpoint to expose a swagger document.
 */

@Produces("application/json")
@Path("/doc")
public class DocEndpoint {
    protected Map<String, String> documents;

    /**
     * Constructs the resource.
     *
     * @param docs Map of path parameter name to swagger document.
     */
    @Inject
    public DocEndpoint(@Named("swagger") Map<String, Swagger> docs) {
        documents = new HashMap<>();

        docs.forEach((key, value) -> {
            documents.put(key, SwaggerBuilder.getDocument(value));
        });
    }

    /**
     * Read handler.
     *
     * @return response The Swagger JSON document
     */
    @GET
    @Path("/{name}")
    public Response get(@PathParam("name") String name) {
        if (documents.containsKey(name)) {
            return Response.status(200).entity(documents.get(name)).build();
        } else {
            return Response.status(404).entity("Unknown document: " + name).build();
        }
    }
}
