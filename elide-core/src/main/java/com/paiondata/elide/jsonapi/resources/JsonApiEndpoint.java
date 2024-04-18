/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi.resources;

import com.paiondata.elide.Elide;
import com.paiondata.elide.ElideResponse;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.request.route.BasicApiVersionValidator;
import com.paiondata.elide.core.request.route.FlexibleRouteResolver;
import com.paiondata.elide.core.request.route.NullRouteResolver;
import com.paiondata.elide.core.request.route.Route;
import com.paiondata.elide.core.request.route.RouteResolver;
import com.paiondata.elide.core.security.User;
import com.paiondata.elide.jsonapi.JsonApi;
import com.paiondata.elide.utils.HeaderProcessor;
import com.paiondata.elide.utils.ResourceUtils;
import org.apache.commons.lang3.StringUtils;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Default endpoint/servlet for using Elide and JSONAPI.
 */
@Singleton
@Produces(JsonApi.MEDIA_TYPE)
@Path("/")
public class JsonApiEndpoint {
    protected final Elide elide;
    protected final JsonApi jsonApi;
    protected final HeaderProcessor headerProcessor;
    protected final RouteResolver routeResolver;

    @Inject
    public JsonApiEndpoint(
            @Named("elide") Elide elide, Optional<RouteResolver> optionalRouteResolver) {
        this.elide = elide;
        this.jsonApi = new JsonApi(this.elide);
        this.headerProcessor = elide.getElideSettings().getHeaderProcessor();
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
     *
     * @param path request path
     * @param uriInfo URI info
     * @param headers the request headers
     * @param securityContext security context
     * @param jsonapiDocument post data as jsonapi document
     * @return response
     */
    @POST
    @Path("{path:.*}")
    @Consumes(JsonApi.MEDIA_TYPE)
    public Response post(
        @PathParam("path") String path,
        @Context UriInfo uriInfo,
        @Context HttpHeaders headers,
        @Context SecurityContext securityContext,
        String jsonapiDocument) {
        Map<String, List<String>> requestHeaders = headerProcessor.process(headers.getRequestHeaders());
        User user = new SecurityContextUser(securityContext);

        String baseUrl = getBaseUrlEndpoint(uriInfo);
        String pathname = path;
        Route route = routeResolver.resolve(JsonApi.MEDIA_TYPE, baseUrl, pathname, requestHeaders,
                uriInfo.getQueryParameters());

        if ("/operations".equals(route.getPath()) || "operations".equals(route.getPath())) {
            // Atomic Operations
            return build(jsonApi.operations(route, jsonapiDocument, user, UUID.randomUUID()));
        }

        return build(jsonApi.post(route, jsonapiDocument, user, UUID.randomUUID()));
    }

    /**
     * Read handler.
     *
     * @param path request path
     * @param uriInfo URI info
     * @param headers the request headers
     * @param securityContext security context
     * @return response
     */
    @GET
    @Path("{path:.*}")
    public Response get(
        @PathParam("path") String path,
        @Context UriInfo uriInfo,
        @Context HttpHeaders headers,
        @Context SecurityContext securityContext) {
        Map<String, List<String>> requestHeaders = headerProcessor.process(headers.getRequestHeaders());
        User user = new SecurityContextUser(securityContext);

        String baseUrl = getBaseUrlEndpoint(uriInfo);
        String pathname = path;
        Route route = routeResolver.resolve(JsonApi.MEDIA_TYPE, baseUrl, pathname, requestHeaders,
                uriInfo.getQueryParameters());

        return build(jsonApi.get(route, user, UUID.randomUUID()));
    }

    /**
     * Update handler.
     *
     * @param contentType document MIME type
     * @param accept response MIME type
     * @param path request path
     * @param uriInfo URI info
     * @param headers the request headers
     * @param securityContext security context
     * @param jsonapiDocument patch data as jsonapi document
     * @return response
     */
    @PATCH
    @Path("{path:.*}")
    @Consumes(JsonApi.MEDIA_TYPE)
    public Response patch(
        @PathParam("path") String path,
        @Context UriInfo uriInfo,
        @Context HttpHeaders headers,
        @Context SecurityContext securityContext,
        String jsonapiDocument) {
        Map<String, List<String>> requestHeaders = headerProcessor.process(headers.getRequestHeaders());
        User user = new SecurityContextUser(securityContext);

        String baseUrl = getBaseUrlEndpoint(uriInfo);
        String pathname = path;
        Route route = routeResolver.resolve(JsonApi.MEDIA_TYPE, baseUrl, pathname, requestHeaders,
                uriInfo.getQueryParameters());

        return build(jsonApi.patch(route, jsonapiDocument, user, UUID.randomUUID()));
    }

    /**
     * Delete relationship handler (expects body with resource ids and types).
     *
     * @param path request path
     * @param uriInfo URI info
     * @param headers the request headers
     * @param securityContext security context
     * @param jsonApiDocument DELETE document
     * @return response
     */
    @DELETE
    @Path("{path:.*}")
    @Consumes(JsonApi.MEDIA_TYPE)
    public Response delete(
        @PathParam("path") String path,
        @Context UriInfo uriInfo,
        @Context HttpHeaders headers,
        @Context SecurityContext securityContext,
        String jsonApiDocument) {
        Map<String, List<String>> requestHeaders = headerProcessor.process(headers.getRequestHeaders());
        User user = new SecurityContextUser(securityContext);

        String baseUrl = getBaseUrlEndpoint(uriInfo);
        String pathname = path;
        Route route = routeResolver.resolve(JsonApi.MEDIA_TYPE, baseUrl, pathname, requestHeaders,
                uriInfo.getQueryParameters());

        return build(jsonApi.delete(route, jsonApiDocument, user, UUID.randomUUID()));
    }

    private static Response build(ElideResponse<String> response) {
        return Response.status(response.getStatus()).entity(response.getBody()).build();
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
