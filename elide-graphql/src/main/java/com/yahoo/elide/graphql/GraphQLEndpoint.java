/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import static com.yahoo.elide.core.EntityDictionary.NO_VERSION;
import static com.yahoo.elide.graphql.QueryRunner.buildErrorResponse;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.resources.SecurityContextUser;
import com.yahoo.elide.security.User;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
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
    private final Map<String, QueryRunner> runners;
    private final Elide elide;

    @Inject
    public GraphQLEndpoint(@Named("elide") Elide elide) {
        log.debug("Started ~~");
        this.elide = elide;
        this.runners = new HashMap<>();
        for (String apiVersion : elide.getElideSettings().getDictionary().getApiVersions()) {
            runners.put(apiVersion, new QueryRunner(elide, apiVersion));
        }
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
            @HeaderParam("ApiVersion") String apiVersion,
            @Context HttpHeaders headers,
            @Context SecurityContext securityContext,
            String graphQLDocument) {

        String safeApiVersion = apiVersion == null ? NO_VERSION : apiVersion;
        User user = new SecurityContextUser(securityContext);
        QueryRunner runner = runners.getOrDefault(safeApiVersion, null);

        ElideResponse response;
        if (runner == null) {
            response = buildErrorResponse(elide, new InvalidOperationException("Invalid API Version"), false);
        } else {
            response = runner.run(uriInfo.getBaseUri().toString(), graphQLDocument, user, UUID.randomUUID(), headers.getRequestHeaders());
        }
        return Response.status(response.getResponseCode()).entity(response.getBody()).build();
    }
}
