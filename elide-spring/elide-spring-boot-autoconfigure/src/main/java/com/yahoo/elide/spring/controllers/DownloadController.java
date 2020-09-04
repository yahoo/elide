/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.spring.controllers;

import com.yahoo.elide.async.service.ResultStorageEngine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;
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

import javax.servlet.http.HttpServletResponse;

@Slf4j
@Configuration
@RestController
@RequestMapping(value = "${elide.async.download.path:download}")
@ConditionalOnExpression("${elide.async.download.enabled:false}")
public class DownloadController {

    private ResultStorageEngine resultStorageEngine;

    @Autowired
    public DownloadController(ResultStorageEngine resultStorageEngine) {
        log.debug("Started ~~");
        this.resultStorageEngine = resultStorageEngine;
    }

    @GetMapping(path = "/{asyncQueryId}")
    public Callable<ResponseEntity<StreamingResponseBody>> download(@PathVariable String asyncQueryId,
            HttpServletResponse response) throws IOException {

        return new Callable<ResponseEntity<StreamingResponseBody>>() {
            @Override
            public ResponseEntity<StreamingResponseBody> call() {

                Observable<String> observableResults = resultStorageEngine.getResultsByID(asyncQueryId);

                StreamingResponseBody streamingOutput = outputStream -> {
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

                if (observableResults.isEmpty().blockingGet()) {
                    return ResponseEntity.notFound().build();
                } else {
                    return ResponseEntity
                            .ok()
                            .header("Content-Disposition", "attachment; filename=" + asyncQueryId)
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .body(streamingOutput);
                }
            }
        };
    }
}
