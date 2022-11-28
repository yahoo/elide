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
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.utils.HeaderUtils;
import com.yahoo.elide.utils.ResourceUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

/**
 * Default endpoint/servlet for using Elide and JSONAPI.
 */
@Singleton
@Produces(JSONAPI_CONTENT_TYPE)
@Path("/")
public class JsonApiEndpoint {
    protected final Elide elide;
    protected final HeaderUtils.HeaderProcessor headerProcessor;

    @Inject
    public JsonApiEndpoint(
            @Named("elide") Elide elide) {
        this.elide = elide;
        this.headerProcessor = elide.getElideSettings().getHeaderProcessor();
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
    @Consumes(JSONAPI_CONTENT_TYPE)
    public Response post(
        @PathParam("path") String path,
        @Context UriInfo uriInfo,
        @Context HttpHeaders headers,
        @Context SecurityContext securityContext,
        String jsonapiDocument) {
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        String apiVersion = HeaderUtils.resolveApiVersion(headers.getRequestHeaders());
        Map<String, List<String>> requestHeaders = headerProcessor.process(headers.getRequestHeaders());
        User user = new SecurityContextUser(securityContext);
        return build(elide.post(getBaseUrlEndpoint(uriInfo), path, jsonapiDocument,
                queryParams, requestHeaders, user, apiVersion, UUID.randomUUID()));
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
        String apiVersion = HeaderUtils.resolveApiVersion(headers.getRequestHeaders());
        Map<String, List<String>> requestHeaders = headerProcessor.process(headers.getRequestHeaders());
        User user = new SecurityContextUser(securityContext);

        return build(elide.get(getBaseUrlEndpoint(uriInfo), path, queryParams,
                               requestHeaders, user, apiVersion, UUID.randomUUID()));
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
    @Consumes(JSONAPI_CONTENT_TYPE)
    public Response patch(
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
        return build(elide.patch(getBaseUrlEndpoint(uriInfo), contentType, accept, path,
                                 jsonapiDocument, queryParams, requestHeaders, user, apiVersion, UUID.randomUUID()));
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
    @Consumes(JSONAPI_CONTENT_TYPE)
    public Response delete(
        @PathParam("path") String path,
        @Context UriInfo uriInfo,
        @Context HttpHeaders headers,
        @Context SecurityContext securityContext,
        String jsonApiDocument) {
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        String apiVersion =
                HeaderUtils.resolveApiVersion(headers.getRequestHeaders());
        Map<String, List<String>> requestHeaders = headerProcessor.process(headers.getRequestHeaders());
        User user = new SecurityContextUser(securityContext);
        return build(elide.delete(getBaseUrlEndpoint(uriInfo), path, jsonApiDocument, queryParams, requestHeaders,
                                  user, apiVersion, UUID.randomUUID()));
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

        return baseUrl;
    }
}
