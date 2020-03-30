/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.resources.SecurityContextUser;
import com.yahoo.elide.security.User;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

/**
 * Default endpoint/servlet for using Elide and JSONAPI.
 */
@Slf4j
@Singleton
@Produces(MediaType.APPLICATION_JSON)
@Path("/")
public class GraphQLEndpoint {
    private final QueryRunner runner;

    @Inject
    public GraphQLEndpoint(@Named("elide") Elide elide) {
        log.debug("Started ~~");
        this.runner = new QueryRunner(elide);
    }

    /**
     * Create handler.
     *
     * @param securityContext security context
     * @param graphQLDocument post data as jsonapi document
     * @return response
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response post(
            @Context SecurityContext securityContext,
            String graphQLDocument) {
        User user = new SecurityContextUser(securityContext);
        ElideResponse response = runner.run(graphQLDocument, user);
        return Response.status(response.getResponseCode()).entity(response.getBody()).build();
    }
}
