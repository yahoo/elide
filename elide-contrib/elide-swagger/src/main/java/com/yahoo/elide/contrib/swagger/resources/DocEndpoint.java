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
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * A convenience endpoint to expose a swagger document.
 */
@Singleton
@Produces("application/json")
@Path("/doc")
public class DocEndpoint {
    protected final String document;

    @Inject
    public DocEndpoint(@Named("swagger") Swagger swagger) {
        this.document = SwaggerBuilder.getDocument(swagger);
    }

    /**
     * Read handler.
     * @return response The Swagger JSON document
     */
    @GET
    public Response get() {
        return Response.status(200).entity(document).build();
    }
}
