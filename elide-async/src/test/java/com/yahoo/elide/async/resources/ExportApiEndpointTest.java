/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.paiondata.elide.async.resources.ExportApiEndpoint.ExportApiProperties;
import com.paiondata.elide.async.service.storageengine.FileResultStorageEngine;
import com.paiondata.elide.async.service.storageengine.ResultStorageEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * ExportAPiEndpoint Test.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ExportApiEndpointTest {

    private ExportApiEndpoint endpoint;
    private ResultStorageEngine engine;
    private AsyncResponse asyncResponse;
    private HttpServletResponse response;
    private ExportApiProperties exportApiProperties;
    private ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);

    @BeforeEach
    public void setup() {
        engine = mock(FileResultStorageEngine.class);
        asyncResponse = mock(AsyncResponse.class);
        response = mock(HttpServletResponse.class);
    }

    @Test
    public void testGet() {
        String queryId = "1";
        int maxDownloadTimeSeconds = 1;
        int maxDownloadTimeMilliSeconds = (int) TimeUnit.SECONDS.toMillis(maxDownloadTimeSeconds);
        when(engine.getResultsByID(queryId)).thenReturn(outputStream -> {
            try {
                outputStream.write("result".getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        exportApiProperties = new ExportApiProperties(Executors.newFixedThreadPool(1), Duration.ofSeconds(maxDownloadTimeSeconds));
        endpoint = new ExportApiEndpoint(engine, exportApiProperties);
        endpoint.get(queryId, response, asyncResponse);

        // Timeout(int) succeeds as soon as the function to be verified is called.
        // It waits maximum upto value of "int" for function to be called.
        verify(engine, timeout(maxDownloadTimeMilliSeconds)).getResultsByID(queryId);
        verify(asyncResponse, timeout(maxDownloadTimeMilliSeconds)).resume(responseCaptor.capture());
        final Response res = responseCaptor.getValue();

        assertEquals(res.getStatus(), 200);
    }
}
