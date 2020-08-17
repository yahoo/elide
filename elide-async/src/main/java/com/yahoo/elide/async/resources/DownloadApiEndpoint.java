/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.resources;

import com.yahoo.elide.async.service.ResultStorageEngine;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

/**
 * Default endpoint/servlet for using Elide and Async Download.
 */
@Singleton
@Path("/")
public class DownloadApiEndpoint {
    protected final ResultStorageEngine resultStorageEngine;

    @Inject
    public DownloadApiEndpoint(
            @Named("resultStorageEngine") ResultStorageEngine resultStorageEngine) {
        this.resultStorageEngine = resultStorageEngine;
    }

    /**
     * Read handler.
     *
     * @param asyncQueryId asyncQueryId to download results
     * @return response Response
     */
    @GET
    @Path("/{asyncQueryId}")
    public Response get(
        @PathParam("asyncQueryId") String asyncQueryId) {
        ///************* Getresults from ResultStorageEngine
        byte[] temp = resultStorageEngine.getResultsByID(asyncQueryId);
        ResponseBuilder response;
        if (temp == null) {
            response = Response.noContent();
        } else {
            response = Response.ok(new String(temp), MediaType.APPLICATION_OCTET_STREAM);
            response.header("Content-Disposition", "attachment; filename=" + asyncQueryId);
        }
        return response.build();
    }
}
