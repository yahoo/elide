/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.resources;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;
import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.annotation.PATCH;
import com.yahoo.elide.core.security.User;

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

    @Inject
    public JsonApiEndpoint(
            @Named("elide") Elide elide) {
        this.elide = elide;
    }

    /**
     * Create handler.
     *
     * @param path request path
     * @param uriInfo URI info
     * @param apiVersion The api version
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
        @HeaderParam("ApiVersion") String apiVersion,
        @Context SecurityContext securityContext,
        String jsonapiDocument) {
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        String safeApiVersion = apiVersion == null ? NO_VERSION : apiVersion;
        User user = new SecurityContextUser(securityContext);
        return build(elide.post(uriInfo.getBaseUri().toString(), path, jsonapiDocument,
                queryParams, user, safeApiVersion, UUID.randomUUID()));
    }

    /**
     * Read handler.
     *
     * @param path request path
     * @param apiVersion The API version
     * @param uriInfo URI info
     * @param securityContext security context
     * @return response
     */
    @GET
    @Path("{path:.*}")
    public Response get(
        @PathParam("path") String path,
        @HeaderParam("ApiVersion") String apiVersion,
        @Context UriInfo uriInfo,
        @Context SecurityContext securityContext) {
        String safeApiVersion = apiVersion == null ? NO_VERSION : apiVersion;
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        User user = new SecurityContextUser(securityContext);
        return build(elide.get(uriInfo.getBaseUri().toString(), path, queryParams, user, safeApiVersion));
    }

    /**
     * Update handler.
     *
     * @param contentType document MIME type
     * @param apiVersion the API version
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
        @HeaderParam("ApiVersion") String apiVersion,
        @HeaderParam("accept") String accept,
        @PathParam("path") String path,
        @Context UriInfo uriInfo,
        @Context SecurityContext securityContext,
        String jsonapiDocument) {
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();

        String safeApiVersion = apiVersion == null ? NO_VERSION : apiVersion;
        User user = new SecurityContextUser(securityContext);
        return build(elide.patch(uriInfo.getBaseUri().toString(), contentType, accept, path,
                                 jsonapiDocument, queryParams, user, safeApiVersion, UUID.randomUUID()));
    }

    /**
     * Delete relationship handler (expects body with resource ids and types).
     *
     * @param path request path
     * @param uriInfo URI info
     * @param apiVersion the API version.
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
        @HeaderParam("ApiVersion") String apiVersion,
        @Context SecurityContext securityContext,
        String jsonApiDocument) {
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        String safeApiVersion = apiVersion == null ? NO_VERSION : apiVersion;
        User user = new SecurityContextUser(securityContext);
        return build(elide.delete(uriInfo.getBaseUri().toString(), path, jsonApiDocument, queryParams,
                user, safeApiVersion, UUID.randomUUID()));
    }

    private static Response build(ElideResponse response) {
        return Response.status(response.getResponseCode()).entity(response.getBody()).build();
    }
}
