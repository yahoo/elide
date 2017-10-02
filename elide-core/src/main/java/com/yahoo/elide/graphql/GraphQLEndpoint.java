/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.resources.DefaultOpaqueUserFunction;
import graphql.GraphQL;
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

import java.util.function.Function;

/**
 * Default endpoint/servlet for using Elide and JSONAPI.
 */
@Slf4j
@Singleton
@Produces(MediaType.APPLICATION_JSON)
@Path("/")
public class GraphQLEndpoint {

    private Elide elide;
    private GraphQL api;
    protected final Function<SecurityContext, Object> getUser;

    @Inject
    public GraphQLEndpoint(
            @Named("elide") Elide elide,
            @Named("elideUserExtractionFunction") DefaultOpaqueUserFunction getUser) {
        log.debug("Mounted GraphQL Endpoint ~~");
        this.elide = elide;
        this.getUser = getUser;
        PersistentResourceFetcher fetcher = new PersistentResourceFetcher(elide.getElideSettings());
        ModelBuilder builder = new ModelBuilder(elide.getElideSettings().getDictionary(), fetcher);
        this.api = new GraphQL(builder.build());
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
        return build(elide.graphqlPost(api, null, graphQLDocument, getUser.apply(securityContext)));
    }

    private static Response build(ElideResponse response) {
        return Response.status(response.getResponseCode()).entity(response.getBody()).build();
    }
}
