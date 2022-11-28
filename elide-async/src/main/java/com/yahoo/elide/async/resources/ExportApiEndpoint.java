/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.resources;

import com.yahoo.elide.async.service.storageengine.ResultStorageEngine;
import com.yahoo.elide.core.exceptions.HttpStatus;

import io.reactivex.Observable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

/**
 * Default endpoint/servlet for using Elide and Async Export.
 */
@Slf4j
@Singleton
@Path("/")
public class ExportApiEndpoint {
    protected final ExportApiProperties exportApiProperties;
    protected final ResultStorageEngine resultStorageEngine;

    @Data
    @AllArgsConstructor
    public static class ExportApiProperties {
        private ExecutorService executor;
        private Integer maxDownloadTimeSeconds;
    }

    @Inject
    public ExportApiEndpoint(
            @Named("resultStorageEngine") ResultStorageEngine resultStorageEngine,
            @Named("exportApiProperties") ExportApiProperties exportApiProperties) {
        this.resultStorageEngine = resultStorageEngine;
        this.exportApiProperties = exportApiProperties;
    }

    /**
     * Read handler.
     *
     * @param asyncQueryId asyncQueryId to download results
     * @param asyncResponse AsyncResponse object
     */
    @GET
    @Path("/{asyncQueryId}")
    public void get(@PathParam("asyncQueryId") String asyncQueryId, @Context HttpServletResponse httpServletResponse,
            @Suspended final AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(exportApiProperties.getMaxDownloadTimeSeconds(), TimeUnit.SECONDS);
        asyncResponse.setTimeoutHandler(async -> {
            ResponseBuilder resp = Response.status(Response.Status.REQUEST_TIMEOUT).entity("Timed out.");
            async.resume(resp.build());
        });

        exportApiProperties.getExecutor().submit(() -> {
            Observable<String> observableResults = resultStorageEngine.getResultsByID(asyncQueryId);

            StreamingOutput streamingOutput = outputStream ->
                observableResults
                .subscribe(
                        resultString -> outputStream.write(resultString.concat(System.lineSeparator()).getBytes()),
                        error -> {
                            String message = error.getMessage();
                            try {
                                log.debug(message);
                                if (message != null && message.equals(ResultStorageEngine.RETRIEVE_ERROR)) {
                                    httpServletResponse.sendError(HttpStatus.SC_NOT_FOUND, asyncQueryId + " Not Found");
                                } else {
                                    httpServletResponse.sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                                }
                            } catch (IllegalStateException e) {
                                // If stream was flushed, Attachment download has already started.
                                // response.sendError causes java.lang.IllegalStateException:
                                // Cannot call sendError() after the response has been committed.
                                // This will return 200 status.
                                // Add error message in the attachment as a way to signal errors.
                                outputStream.write(
                                        "Error Occured...."
                                        .concat(System.lineSeparator())
                                        .getBytes()
                                        );
                                log.debug(e.getMessage());
                            } finally {
                                outputStream.flush();
                                outputStream.close();
                            }
                        },
                        () -> {
                            outputStream.flush();
                            outputStream.close();
                        }
                );

            asyncResponse.resume(Response.ok(streamingOutput, MediaType.APPLICATION_OCTET_STREAM)
                    .header("Content-Disposition", "attachment; filename=" + asyncQueryId).build());
        });
    }
}
