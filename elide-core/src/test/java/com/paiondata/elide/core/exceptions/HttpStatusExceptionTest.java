/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.paiondata.elide.ElideErrorResponse;
import com.paiondata.elide.ElideErrors;

import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

class HttpStatusExceptionTest {

    @Test
    void testGetResponse() {
        String expected = "test<script>encoding";
        HttpStatusException exception =  new HttpStatusException(500, "test<script>encoding") { };
        ElideErrorResponse<?> res = exception.getErrorResponse();
        assertEquals(expected, res.getBody(ElideErrors.class).getErrors().get(0).getMessage());
    }

    @Test
    void testGetVerboseResponse() {
        String expected = "test<script>encoding";
        HttpStatusException exception = new HttpStatusException(500, "test<script>encoding") { };
        ElideErrorResponse<?> res = exception.getVerboseErrorResponse();
        assertEquals(expected, res.getBody(ElideErrors.class).getErrors().get(0).getMessage());
    }

    @Test
    void testGetVerboseResponseWithSupplier() {
        String expected = """
                test<script>encoding
                a more verbose <script> encoding test""";
        Supplier<String> supplier = () -> "a more verbose <script> encoding test";
        HttpStatusException exception = new HttpStatusException(500, "test<script>encoding",
                new RuntimeException("runtime exception"), supplier) { };
        ElideErrorResponse<?> res = exception.getVerboseErrorResponse();
        assertEquals(expected, res.getBody(ElideErrors.class).getErrors().get(0).getMessage());
    }
}
