/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.resources.DefaultOpaqueUserFunction;

import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;

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
import javax.ws.rs.core.UriInfo;

/**
 * Default endpoint/servlet for using Elide and JSONAPI.
 */
@Slf4j
@Singleton
@Produces(MediaType.APPLICATION_JSON)
@Path("/")
public class GraphQLEndpoint {
    private static final DefaultOpaqueUserFunction DEFAULT_GET_USER = securityContext -> securityContext;

    protected final Function<SecurityContext, Object> getUser;

    private final QueryRunner runner;

    @Inject
    public GraphQLEndpoint(
            @Named("elide") Elide elide,
            @Named("elideUserExtractionFunction") DefaultOpaqueUserFunction getUser) {
        log.debug("Started ~~");
        this.runner = new QueryRunner(elide);
        this.getUser = getUser == null ? DEFAULT_GET_USER : getUser;
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
            @Context UriInfo uriInfo,
            @Context SecurityContext securityContext,
            String graphQLDocument) {

        ElideResponse response = runner.run(uriInfo.getBaseUri().toString(),
                graphQLDocument, getUser.apply(securityContext));
        return Response.status(response.getResponseCode()).entity(response.getBody()).build();
    }
}
