/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.controllers;

import com.paiondata.elide.async.service.storageengine.ResultStorageEngine;
import com.paiondata.elide.core.exceptions.HttpStatus;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Consumer;

/**
 * Spring rest controller for Elide Export.
 * When enabled it is highly recommended to
 * configure explicitly the TaskExecutor used in Spring MVC for executing
 * asynchronous requests using StreamingResponseBody. Refer
 * {@link org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody}.
 */
@Slf4j
@RestController
@RequestMapping(value = "${elide.async.export.path:/export}")
public class ExportController {

    private ResultStorageEngine resultStorageEngine;

    public ExportController(ResultStorageEngine resultStorageEngine) {
        log.debug("Started ~~");
        this.resultStorageEngine = resultStorageEngine;
    }

    /**
     * Single entry point for export requests.
     * @param asyncQueryId Id of results to download
     * @param response HttpServletResponse instance
     * @return ResponseEntity
     */
    @GetMapping(path = "/{asyncQueryId}")
    public ResponseEntity<StreamingResponseBody> export(@PathVariable String asyncQueryId,
            HttpServletResponse response) {
        Consumer<OutputStream> observableResults = resultStorageEngine.getResultsByID(asyncQueryId);
        StreamingResponseBody streamingOutput = outputStream -> {
            try {
                observableResults.accept(outputStream);
            } catch (RuntimeException e) {
                String message = e.getMessage();
                try {
                    log.debug(message);
                    if (message != null && message.equals(ResultStorageEngine.RETRIEVE_ERROR)) {
                        response.sendError(HttpStatus.SC_NOT_FOUND, asyncQueryId + "not found");
                    } else {
                        response.sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                    }
                } catch (IOException | IllegalStateException ie) {
                    // If stream was flushed, Attachment download has already started.
                    // response.sendError causes java.lang.IllegalStateException:
                    // Cannot call sendError() after the response has been committed.
                    // This will return 200 status.
                    // Add error message in the attachment as a way to signal errors.
                    outputStream.write("Error Occured....".concat(System.lineSeparator()).getBytes());
                    log.debug(ie.getMessage());
                } finally {
                    outputStream.flush();
                    outputStream.close();
                }
            }
        };
        return ResponseEntity
                .ok()
                .header("Content-Disposition", "attachment; filename=" + asyncQueryId)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(streamingOutput);
    }
}
