/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.export.formatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.async.models.QueryType;
import com.yahoo.elide.async.models.ResultType;
import com.yahoo.elide.async.models.TableExport;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.security.checks.Check;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class JSONExportFormatterTest {
   public static final String FORMAT = "yyyy-MM-dd'T'HH:mm'Z'";
    private static final SimpleDateFormat FORMATTER = new SimpleDateFormat(FORMAT);
    private HashMapDataStore dataStore;
    private Elide elide;

   @BeforeEach
    public void setupMocks(@TempDir Path tempDir) {
        dataStore = new HashMapDataStore(TableExport.class.getPackage());
        Map<String, Class<? extends Check>> map = new HashMap<>();
        elide = new Elide(
                    new ElideSettingsBuilder(dataStore)
                        .withEntityDictionary(new EntityDictionary(map))
                        .withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"))
                        .build());
        FORMATTER.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    @Test
    public void testResourceToJSON() {
        JSONExportFormatter formatter = new JSONExportFormatter(elide);
        TableExport queryObj = new TableExport();
        String query = "/tableExport";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.GRAPHQL_V1_0);
        queryObj.setResultType(ResultType.CSV);

        String start = "{\"id\":\"edc4a871-dff2-4054-804e-d80075cf827d\",\"query\":\"/tableExport\","
                + "\"queryType\":\"GRAPHQL_V1_0\",\"requestId\":\"" + queryObj.getRequestId() + "\",\"principalName\":null"
                + ",\"status\":\""  + queryObj.getStatus() + "\",\"createdOn\":\"" + FORMATTER.format(queryObj.getCreatedOn())
                + "\",\"updatedOn\":\"" + FORMATTER.format(queryObj.getUpdatedOn()) + "\",\"asyncAfterSeconds\":10,\"resultType\":\"CSV\","
                + "\"result\":null}";

        PersistentResource resource = mock(PersistentResource.class);
        when(resource.getObject()).thenReturn(queryObj);
        String output = formatter.format(resource, 1);
        assertEquals(true, output.contains(start));
    }
}
