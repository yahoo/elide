/*
 * Copyright 2018, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.jsonapi;

import com.yahoo.elide.utils.coerce.CoerceUtil;
import com.yahoo.elide.utils.coerce.converters.ISO8601DateSerde;
import com.yahoo.elide.utils.coerce.converters.Serde;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.TimeZone;

public class JsonApiMapperTest {
    private Serde oldSerde;

    @BeforeClass
    public void init() {

        oldSerde = CoerceUtil.lookup(Date.class);

        CoerceUtil.register(Date.class, new ISO8601DateSerde(
                "yyyy-MM-dd'T'HH:mm'Z'",
                TimeZone.getTimeZone("UTC"),
                java.sql.Date.class));
    }

    @AfterClass
    public void cleanup() {
        CoerceUtil.register(Date.class, oldSerde);
    }

    @Test
    public void testSqlDateSerialization() throws Exception {
        JsonApiMapper jsonApiMapper = new JsonApiMapper();

        ObjectMapper mapper = jsonApiMapper.getObjectMapper();

        String result = mapper.writeValueAsString(new java.sql.Date(0));
        Assert.assertEquals(result, "\"1970-01-01T00:00Z\"");
    }

    @Test
    public void testlDateSerialization() throws Exception {
        JsonApiMapper jsonApiMapper = new JsonApiMapper();

        ObjectMapper mapper = jsonApiMapper.getObjectMapper();

        String result = mapper.writeValueAsString(new Date(0));
        Assert.assertEquals(result, "\"1970-01-01T00:00Z\"");
    }

    @Test
    public void testSqlTimestampSerialization() throws Exception {
        JsonApiMapper jsonApiMapper = new JsonApiMapper();

        ObjectMapper mapper = jsonApiMapper.getObjectMapper();

        String result = mapper.writeValueAsString(new java.sql.Timestamp(0));
        Assert.assertEquals(result, "\"1970-01-01T00:00Z\"");
    }

    @Test
    public void testSqlTimeSerialization() throws Exception {
        JsonApiMapper jsonApiMapper = new JsonApiMapper();

        ObjectMapper mapper = jsonApiMapper.getObjectMapper();

        String result = mapper.writeValueAsString(new java.sql.Time(0));
        Assert.assertEquals(result, "\"1970-01-01T00:00Z\"");
    }
}
