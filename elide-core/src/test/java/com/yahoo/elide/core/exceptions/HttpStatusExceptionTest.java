/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

public class HttpStatusExceptionTest {

    @Test
    public void testGetEncodedResponse() {
        String expected = "{\"errors\":[{\"detail\":\"test&lt;script&gt;encoding\"}]}";
        HttpStatusException exception =  new HttpStatusException(500, "test<script>encoding") { };
        Pair<Integer, JsonNode> res = exception.getErrorResponse();
        assertEquals(expected, res.getRight().toString());
    }

    @Test
    public void testGetEncodedVerboseResponse() {
        String expected = "{\"errors\":[{\"detail\":\"test&lt;script&gt;encoding\"}]}";
        HttpStatusException exception = new HttpStatusException(500, "test<script>encoding") { };
        Pair<Integer, JsonNode> res = exception.getVerboseErrorResponse();
        assertEquals(expected, res.getRight().toString());
    }

    @Test
    public void testGetEncodedVerboseResponseWithSupplier() {
        String expected = "{\"errors\":[{\"detail\":\"test&lt;script&gt;encoding\\na more verbose &lt;script&gt; encoding test\"}]}";
        Supplier<String> supplier = () -> "a more verbose <script> encoding test";
        HttpStatusException exception = new HttpStatusException(500, "test<script>encoding",
                new RuntimeException("runtime exception"), supplier) { };
        Pair<Integer, JsonNode> res = exception.getVerboseErrorResponse();
        assertEquals(expected, res.getRight().toString());
    }

    @Test
    public void testGetVerboseResponseWithSupplier() {
        String expected = "{\"errors\":[{\"detail\":\"test&lt;script&gt;encoding\\na more verbose &lt;script&gt; encoding test\"}]}";
        Supplier<String> supplier = () -> "a more verbose <script> encoding test";
        HttpStatusException exception = new HttpStatusException(500, "test<script>encoding",
                new RuntimeException("runtime exception"), supplier) { };
        Pair<Integer, JsonNode> res = exception.getVerboseErrorResponse();
        assertEquals(expected, res.getRight().toString());
    }
}
