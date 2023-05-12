/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.resources;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.annotation.PATCH;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.request.route.BasicApiVersionValidator;
import com.yahoo.elide.core.request.route.FlexibleRouteResolver;
import com.yahoo.elide.core.request.route.NullRouteResolver;
import com.yahoo.elide.core.request.route.Route;
import com.yahoo.elide.core.request.route.RouteResolver;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.jsonapi.JsonApi;
import com.yahoo.elide.utils.HeaderUtils;
import com.yahoo.elide.utils.ResourceUtils;
import org.apache.commons.lang3.StringUtils;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
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
@Produces(JSONAPI_CONTENT_TYPE)
@Path("/")
public class JsonApiEndpoint {
    protected final Elide elide;
    protected final HeaderUtils.HeaderProcessor headerProcessor;
    protected final RouteResolver routeResolver;

    @Inject
    public JsonApiEndpoint(
            @Named("elide") Elide elide, Optional<RouteResolver> optionalRouteResolver) {
        this.elide = elide;
        this.headerProcessor = elide.getElideSettings().getHeaderProcessor();
        this.routeResolver = optionalRouteResolver.orElseGet(() -> {
            Set<String> apiVersions = elide.getElideSettings().getDictionary().getApiVersions();
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
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        Map<String, List<String>> requestHeaders = headerProcessor.process(headers.getRequestHeaders());
        User user = new SecurityContextUser(securityContext);

        String baseUrl = getBaseUrlEndpoint(uriInfo);
        String pathname = path;
        Route route = routeResolver.resolve(JSONAPI_CONTENT_TYPE, baseUrl, pathname, requestHeaders,
                uriInfo.getQueryParameters());

        return build(elide.post(route.getBaseUrl(), route.getPath(), jsonapiDocument,
                queryParams, requestHeaders, user, route.getApiVersion(), UUID.randomUUID()));
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
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        Map<String, List<String>> requestHeaders = headerProcessor.process(headers.getRequestHeaders());
        User user = new SecurityContextUser(securityContext);

        String baseUrl = getBaseUrlEndpoint(uriInfo);
        String pathname = path;
        Route route = routeResolver.resolve(JSONAPI_CONTENT_TYPE, baseUrl, pathname, requestHeaders,
                uriInfo.getQueryParameters());

        return build(elide.get(route.getBaseUrl(), route.getPath(), queryParams,
                               requestHeaders, user, route.getApiVersion(), UUID.randomUUID()));
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
        @HeaderParam("Content-Type") String contentType,
        @HeaderParam("accept") String accept,
        @PathParam("path") String path,
        @Context UriInfo uriInfo,
        @Context HttpHeaders headers,
        @Context SecurityContext securityContext,
        String jsonapiDocument) {
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        Map<String, List<String>> requestHeaders = headerProcessor.process(headers.getRequestHeaders());
        User user = new SecurityContextUser(securityContext);

        String baseUrl = getBaseUrlEndpoint(uriInfo);
        String pathname = path;
        Route route = routeResolver.resolve(JSONAPI_CONTENT_TYPE, baseUrl, pathname, requestHeaders,
                uriInfo.getQueryParameters());

        return build(elide.patch(route.getBaseUrl(), contentType, accept, route.getPath(), jsonapiDocument, queryParams,
                requestHeaders, user, route.getApiVersion(), UUID.randomUUID()));
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
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        Map<String, List<String>> requestHeaders = headerProcessor.process(headers.getRequestHeaders());
        User user = new SecurityContextUser(securityContext);

        String baseUrl = getBaseUrlEndpoint(uriInfo);
        String pathname = path;
        Route route = routeResolver.resolve(JSONAPI_CONTENT_TYPE, baseUrl, pathname, requestHeaders,
                uriInfo.getQueryParameters());

        return build(elide.delete(route.getBaseUrl(), route.getPath(), jsonApiDocument, queryParams, requestHeaders,
                                  user, route.getApiVersion(), UUID.randomUUID()));
    }

    /**
     * Operations handler.
     *
     * @param path request path
     * @param uriInfo URI info
     * @param headers the request headers
     * @param securityContext security context
     * @param jsonapiDocument post data as jsonapi document
     * @return response
     */
    @POST
    @Path("/operations")
    @Consumes(JsonApi.AtomicOperations.MEDIA_TYPE)
    public Response operations(
        @HeaderParam("Content-Type") String contentType,
        @HeaderParam("accept") String accept,
        @PathParam("path") String path,
        @Context UriInfo uriInfo,
        @Context HttpHeaders headers,
        @Context SecurityContext securityContext,
        String jsonapiDocument) {
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        String apiVersion = HeaderUtils.resolveApiVersion(headers.getRequestHeaders());
        Map<String, List<String>> requestHeaders = headerProcessor.process(headers.getRequestHeaders());
        User user = new SecurityContextUser(securityContext);
        return build(elide.operations(getBaseUrlEndpoint(uriInfo), contentType, accept, path, jsonapiDocument,
                queryParams, requestHeaders, user, apiVersion, UUID.randomUUID()));
    }

    private static Response build(ElideResponse response) {
        return Response.status(response.getResponseCode()).entity(response.getBody()).build();
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
