/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import static com.yahoo.elide.graphql.QueryRunner.buildErrorResponse;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.jsonapi.resources.SecurityContextUser;
import com.yahoo.elide.utils.HeaderUtils;
import com.yahoo.elide.utils.ResourceUtils;
import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
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
    private final HeaderUtils.HeaderProcessor headerProcessor;

    @Inject
    public GraphQLEndpoint(@Named("elide") Elide elide) {
        log.debug("Started ~~");
        this.elide = elide;
        this.headerProcessor = elide.getElideSettings().getHeaderProcessor();
        this.runners = new HashMap<>();
        for (String apiVersion : elide.getElideSettings().getDictionary().getApiVersions()) {
            runners.put(apiVersion, new QueryRunner(elide, apiVersion));
        }
    }

    /**
     * Create handler.
     * @param uriInfo URI info
     * @param headers the request headers
     * @param securityContext security context
     * @param graphQLDocument post data as jsonapi document
     * @return response
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response post(
            @Context UriInfo uriInfo,
            @Context HttpHeaders headers,
            @Context SecurityContext securityContext,
            String graphQLDocument) {
        String apiVersion = HeaderUtils.resolveApiVersion(headers.getRequestHeaders());
        Map<String, List<String>> requestHeaders = headerProcessor.process(headers.getRequestHeaders());
        User user = new SecurityContextUser(securityContext);
        QueryRunner runner = runners.getOrDefault(apiVersion, null);

        ElideResponse response;
        if (runner == null) {
            response = buildErrorResponse(elide.getMapper().getObjectMapper(),
                    new InvalidOperationException("Invalid API Version"), false);
        } else {
            response = runner.run(getBaseUrlEndpoint(uriInfo),
                                  graphQLDocument, user, UUID.randomUUID(), requestHeaders);
        }
        return Response.status(response.getResponseCode()).entity(response.getBody()).build();
    }

    protected String getBaseUrlEndpoint(UriInfo uriInfo) {
        String baseUrl = elide.getElideSettings().getBaseUrl();

        if (StringUtils.isEmpty(baseUrl)) {
            //UriInfo has full path appended here already.
            baseUrl = ResourceUtils.resolveBaseUrl(uriInfo);
        }

        return baseUrl;
    }
}
