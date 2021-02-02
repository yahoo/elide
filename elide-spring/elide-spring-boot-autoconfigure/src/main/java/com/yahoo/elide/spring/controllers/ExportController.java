/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.controllers;

import com.yahoo.elide.async.service.storageengine.ResultStorageEngine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import io.reactivex.Observable;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletResponse;

/**
 * Spring rest controller for Elide Export.
 */
@Slf4j
@Configuration
@RestController
@RequestMapping(value = "${elide.async.export.path:/export}")
@ConditionalOnExpression("${elide.async.export.enabled:false}")
public class ExportController {

    private ResultStorageEngine resultStorageEngine;

    @Autowired
    public ExportController(ResultStorageEngine resultStorageEngine) {
        log.debug("Started ~~");
        this.resultStorageEngine = resultStorageEngine;
    }

    @GetMapping(path = "/{asyncQueryId}")
    public Callable<ResponseEntity<StreamingResponseBody>> export(@PathVariable String asyncQueryId,
            HttpServletResponse response) throws IOException {

        return () -> {
            Observable<String> observableResults = resultStorageEngine.getResultsByID(asyncQueryId);
            AtomicInteger recordCount = new AtomicInteger(0);

            StreamingResponseBody streamingOutput = outputStream -> {
                observableResults
                    .map(record -> record)
                    .subscribe(
                        resultString -> {
                            outputStream.write(resultString.concat(System.getProperty("line.separator")).getBytes());
                            recordCount.getAndIncrement();
                        },
                        error -> {
                            if (recordCount.get() != 0) {
                                // Add error message in the attachment as a way to signal errors.
                                // Attachment download has already started.
                                // sendError can not be called,
                                // as it will result in "Cannot call sendError() after the response has been committed".
                                // This will return 200 status.
                                outputStream.write(
                                        "Error Occured...."
                                        .concat(System.getProperty("line.separator"))
                                        .getBytes()
                                );
                                outputStream.flush();
                                outputStream.close();
                                return;
                            }

                            String message = error.getMessage();
                            if (message != null && message.equals(ResultStorageEngine.RETRIEVE_ERROR)) {
                                response.sendError(HttpStatus.NOT_FOUND.value());
                            } else {
                                response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value());
                            }
                        },
                        () -> {
                            outputStream.flush();
                            outputStream.close();
                        }
                    );
            };

           return ResponseEntity
                    .ok()
                    .header("Content-Disposition", "attachment; filename=" + asyncQueryId)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(streamingOutput);
        };
    }
}
