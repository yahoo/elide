/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.resources;

import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.async.service.ResultStorageEngine;
import io.reactivex.Observable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.container.TimeoutHandler;
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
    protected final ExecutorService executor;
    protected final ResultStorageEngine resultStorageEngine;
    protected final Integer maxDownloadTimeSeconds;

    @Inject
    public DownloadApiEndpoint(
            @Named("resultStorageEngine") ResultStorageEngine resultStorageEngine,
            @Named("asyncExecutorService") AsyncExecutorService asyncExecutorService,
            @Named("maxDownloadTimeSeconds") Integer maxDownloadTimeSeconds) {
        this.resultStorageEngine = resultStorageEngine;
        this.executor = asyncExecutorService.getExecutor();
        this.maxDownloadTimeSeconds = maxDownloadTimeSeconds;
    }

    /**
     * Read handler.
     *
     * @param asyncQueryId asyncQueryId to download results
     * @param asyncResponse AsyncResponse object
     */
    @GET
    @Path("/{asyncQueryId}")
    public void get(@PathParam("asyncQueryId") String asyncQueryId,
            @Suspended final AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(maxDownloadTimeSeconds, TimeUnit.SECONDS);
        asyncResponse.setTimeoutHandler(new TimeoutHandler() {
            @Override
            public void handleTimeout(AsyncResponse asyncResponse) {
                ResponseBuilder resp = Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("Timed out.");
                asyncResponse.resume(resp.build());
            }
        });

        executor.submit(() -> {
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
        });
    }
}
