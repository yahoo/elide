/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.resources;

import com.yahoo.elide.async.service.ResultStorageEngine;

import org.glassfish.jersey.server.ManagedAsync;

import io.reactivex.Observable;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;


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
     * @param asyncResponse AsyncResponse object
     */
    @GET
    @Path("/{asyncQueryId}")
    @ManagedAsync
    public void get(@PathParam("asyncQueryId") String asyncQueryId,
            @Suspended final AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(Integer.MAX_VALUE, TimeUnit.MICROSECONDS); //Roughly 35 minutes

        Observable<String> observableResults = resultStorageEngine.getResultsByID(asyncQueryId);

        final StreamingOutput streamingOutput = outputStream -> {
            observableResults.doOnComplete(() -> {
                outputStream.flush();
                outputStream.close();
            });

            observableResults.doOnNext((resultString) -> {
                outputStream.write(resultString.getBytes());
            });

            observableResults.doOnError((error) -> {
                outputStream.flush();
                outputStream.close();
                throw new IllegalStateException(error);
            });
        };

        ResponseBuilder response;

        if (observableResults.isEmpty().blockingGet()) {
            response = Response.status(Response.Status.NOT_FOUND).entity("Result not found");
        } else {
            response = Response.ok(streamingOutput, MediaType.APPLICATION_OCTET_STREAM);
            response.header("Content-Disposition", "attachment; filename=" + asyncQueryId);
        }
        asyncResponse.resume(response.build());
    }
}
