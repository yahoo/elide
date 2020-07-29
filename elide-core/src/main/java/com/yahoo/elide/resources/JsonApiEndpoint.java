/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.resources;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.annotation.PATCH;

import java.util.function.Function;

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
    protected final Function<SecurityContext, Object> getUser;

    public static final DefaultOpaqueUserFunction DEFAULT_GET_USER = securityContext -> securityContext;

    @Inject
    public JsonApiEndpoint(
            @Named("elide") Elide elide,
            @Named("elideUserExtractionFunction") DefaultOpaqueUserFunction getUser) {
        this.elide = elide;
        this.getUser = getUser == null ? DEFAULT_GET_USER : getUser;
    }

    /**
     * Create handler.
     *
     * @param path request path
     * @param uriInfo URI info
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
        @Context SecurityContext securityContext,
        String jsonapiDocument) {
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        return build(elide.post(uriInfo.getBaseUri().toString(), path, jsonapiDocument,
                queryParams, getUser.apply(securityContext)));
    }

    /**
     * Read handler.
     *
     * @param path request path
     * @param uriInfo URI info
     * @param securityContext security context
     * @return response
     */
    @GET
    @Path("{path:.*}")
    public Response get(
        @PathParam("path") String path,
        @Context UriInfo uriInfo,
        @Context SecurityContext securityContext) {
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        return build(elide.get(uriInfo.getBaseUri().toString(), path, queryParams, getUser.apply(securityContext)));
    }

    /**
     * Update handler.
     *
     * @param contentType document MIME type
     * @param accept response MIME type
     * @param path request path
     * @param uriInfo URI info
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
        @Context SecurityContext securityContext,
        String jsonapiDocument) {
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        return build(elide.patch(uriInfo.getBaseUri().toString(), contentType, accept, path, jsonapiDocument,
                                 queryParams, getUser.apply(securityContext)));
    }

    /**
     * Delete relationship handler (expects body with resource ids and types).
     *
     * @param path request path
     * @param uriInfo URI info
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
        @Context SecurityContext securityContext,
        String jsonApiDocument) {
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        return build(elide.delete(uriInfo.getBaseUri().toString(), path, jsonApiDocument,
                queryParams, getUser.apply(securityContext)));
    }

    private static Response build(ElideResponse response) {
        return Response.status(response.getResponseCode()).entity(response.getBody()).build();
    }
}
