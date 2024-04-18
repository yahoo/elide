/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.graphql;

import com.paiondata.elide.Elide;
import com.paiondata.elide.ElideResponse;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.exceptions.InvalidApiVersionException;
import com.paiondata.elide.core.request.route.BasicApiVersionValidator;
import com.paiondata.elide.core.request.route.FlexibleRouteResolver;
import com.paiondata.elide.core.request.route.NullRouteResolver;
import com.paiondata.elide.core.request.route.Route;
import com.paiondata.elide.core.request.route.RouteResolver;
import com.paiondata.elide.core.security.User;
import com.paiondata.elide.jsonapi.resources.SecurityContextUser;
import com.paiondata.elide.utils.HeaderProcessor;
import com.paiondata.elide.utils.ResourceUtils;
import org.apache.commons.lang3.StringUtils;

import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.SimpleDataFetcherExceptionHandler;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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
    private final HeaderProcessor headerProcessor;
    protected final RouteResolver routeResolver;

    @Inject
    public GraphQLEndpoint(@Named("elide") Elide elide,
            Optional<DataFetcherExceptionHandler> optionalDataFetcherExceptionHandler,
            Optional<RouteResolver> optionalRouteResolver
            ) {
        log.debug("Started ~~");
        this.elide = elide;
        this.headerProcessor = elide.getElideSettings().getHeaderProcessor();
        this.runners = new HashMap<>();
        for (String apiVersion : elide.getElideSettings().getEntityDictionary().getApiVersions()) {
            runners.put(apiVersion, new QueryRunner(elide, apiVersion,
                    optionalDataFetcherExceptionHandler.orElseGet(SimpleDataFetcherExceptionHandler::new)));
        }
        this.routeResolver = optionalRouteResolver.orElseGet(() -> {
            Set<String> apiVersions = elide.getElideSettings().getEntityDictionary().getApiVersions();
            if (apiVersions.size() == 1 && apiVersions.contains(EntityDictionary.NO_VERSION)) {
                return new NullRouteResolver();
            } else {
                return new FlexibleRouteResolver(new BasicApiVersionValidator(), elide.getElideSettings()::getBaseUrl);
            }
        });
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
    @Path("{path:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response post(
            @PathParam("path") String path,
            @Context UriInfo uriInfo,
            @Context HttpHeaders headers,
            @Context SecurityContext securityContext,
            String graphQLDocument) {
        Map<String, List<String>> requestHeaders = headerProcessor.process(headers.getRequestHeaders());
        User user = new SecurityContextUser(securityContext);

        String baseUrl = getBaseUrlEndpoint(uriInfo);
        String pathname = path;
        Route route = routeResolver.resolve(MediaType.APPLICATION_JSON, baseUrl, pathname, requestHeaders,
                uriInfo.getQueryParameters());

        QueryRunner runner = runners.getOrDefault(route.getApiVersion(), null);

        ElideResponse<String> response;
        if (runner == null) {
            response = QueryRunner.handleRuntimeException(elide,
                    new InvalidApiVersionException("Invalid API Version"));
        } else {
            response = runner.run(route.getBaseUrl(),
                                  graphQLDocument, user, UUID.randomUUID(), requestHeaders);
        }
        return Response.status(response.getStatus()).entity(response.getBody()).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response post(
            @Context UriInfo uriInfo,
            @Context HttpHeaders headers,
            @Context SecurityContext securityContext,
            String graphQLDocument) {
        return post("", uriInfo, headers, securityContext, graphQLDocument);
    }

    protected String getBaseUrlEndpoint(UriInfo uriInfo) {
        String baseUrl = elide.getElideSettings().getBaseUrl();
        if (StringUtils.isEmpty(baseUrl)) {
            //UriInfo has full path appended here already.
            baseUrl = ResourceUtils.resolveBaseUrl(uriInfo);
        }
        String path = uriInfo.getBaseUri().getPath();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return baseUrl + path;
    }
}
