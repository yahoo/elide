/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.exceptions;

import com.fasterxml.jackson.databind.JsonNode;

import org.apache.commons.lang3.tuple.Pair;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.function.Supplier;

public class HttpStatusExceptionTest {

    @Test
    public void testGetResponse() {
        // result should not be encoded
        String expected = "{\"errors\":[\": test<script>encoding\"]}";
        HttpStatusException exception =  new HttpStatusException(500, "test<script>encoding") { };
        Pair<Integer, JsonNode> res = exception.getErrorResponse();
        Assert.assertEquals(res.getRight().toString(), expected);
    }

    @Test
    public void testGetVerboseResponse() {
        // result should not be encoded
        String expected = "{\"errors\":[\": test<script>encoding\"]}";
        HttpStatusException exception =  new HttpStatusException(500, "test<script>encoding") { };
        Pair<Integer, JsonNode> res = exception.getVerboseErrorResponse();
        Assert.assertEquals(res.getRight().toString(), expected);
    }

    @Test
    public void testGetEncodedResponse() {
        String expected = "{\"errors\":[\": test&lt;script&gt;encoding\"]}";
        HttpStatusException exception =  new HttpStatusException(500, "test<script>encoding") { };
        Pair<Integer, JsonNode> res = exception.getErrorResponse(true);
        Assert.assertEquals(res.getRight().toString(), expected);
    }

    @Test
    public void testGetEncodedVerboseResponse() {
        String expected = "{\"errors\":[\": test&lt;script&gt;encoding\"]}";
        HttpStatusException exception = new HttpStatusException(500, "test<script>encoding") { };
        Pair<Integer, JsonNode> res = exception.getVerboseErrorResponse(true);
        Assert.assertEquals(res.getRight().toString(), expected);
    }

    @Test
    public void testGetEncodedVerboseResponseWithSupplier() {
        String expected = "{\"errors\":[\"a more verbose &lt;script&gt; encoding test\"]}";
        Supplier<String> supplier = () -> "a more verbose <script> encoding test";
        HttpStatusException exception = new HttpStatusException(500, "test<script>encoding",
                new RuntimeException("runtime exception"), supplier) { };
        Pair<Integer, JsonNode> res = exception.getVerboseErrorResponse(true);
        Assert.assertEquals(res.getRight().toString(), expected);
    }

    @Test
    public void testGetVerboseResponseWithSupplier() {
        String expected = "{\"errors\":[\"a more verbose &lt;script&gt; encoding test\"]}";
        Supplier<String> supplier = () -> "a more verbose <script> encoding test";
        HttpStatusException exception = new HttpStatusException(500, "test<script>encoding",
                new RuntimeException("runtime exception"), supplier) { };
        Pair<Integer, JsonNode> res = exception.getVerboseErrorResponse(true);
        Assert.assertEquals(res.getRight().toString(), expected);
    }
}
