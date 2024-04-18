/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.resources;

import com.paiondata.elide.async.service.storageengine.ResultStorageEngine;
import com.paiondata.elide.core.exceptions.HttpStatus;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.StreamingOutput;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.OutputStream;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

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
        private Duration maxDownloadTime;
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
        asyncResponse.setTimeout(exportApiProperties.getMaxDownloadTime().toSeconds(), TimeUnit.SECONDS);
        asyncResponse.setTimeoutHandler(async -> {
            ResponseBuilder resp = Response.status(Response.Status.REQUEST_TIMEOUT).entity("Timed out.");
            async.resume(resp.build());
        });

        exportApiProperties.getExecutor().submit(() -> {
            Consumer<OutputStream> observableResults = resultStorageEngine.getResultsByID(asyncQueryId);
            StreamingOutput streamingOutput = outputStream -> {
                try {
                    observableResults.accept(outputStream);
                } catch (RuntimeException e) {
                    String message = e.getMessage();
                    try {
                        log.debug(message);
                        if (message != null && message.equals(ResultStorageEngine.RETRIEVE_ERROR)) {
                            httpServletResponse.sendError(HttpStatus.SC_NOT_FOUND, asyncQueryId + " Not Found");
                        } else {
                            httpServletResponse.sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                        }
                    } catch (IllegalStateException ise) {
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
                        log.debug(ise.getMessage());
                    }
                } finally {
                    outputStream.flush();
                    outputStream.close();
                }
            };
            asyncResponse.resume(Response.ok(streamingOutput, MediaType.APPLICATION_OCTET_STREAM)
                    .header("Content-Disposition", "attachment; filename=" + asyncQueryId).build());
        });
    }
}
