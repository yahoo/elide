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
import com.yahoo.elide.core.request.Attribute;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.security.checks.Check;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

public class CSVExportFormatterTest {
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
    public void testResourceToCSV() {
        CSVExportFormatter formatter = new CSVExportFormatter(elide, false);
        TableExport queryObj = new TableExport();
        String query = "{ tableExport { edges { node { id query queryType requestId principalName status createdOn updatedOn"
                + " asyncAfterSeconds resultType result} } } }";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.GRAPHQL_V1_0);
        queryObj.setResultType(ResultType.CSV);

        String row = "\"edc4a871-dff2-4054-804e-d80075cf827d\", \"{ tableExport { edges { node { id query queryType requestId"
                + " principalName status createdOn updatedOn asyncAfterSeconds resultType result} } } }\", \"GRAPHQL_V1_0\""
                + ", \"" + queryObj.getRequestId() + "\", " + "null" + ", \"" + queryObj.getStatus() + "\", \"" + FORMATTER.format(queryObj.getCreatedOn())
                + "\", \"" + FORMATTER.format(queryObj.getUpdatedOn()) + "\", " + "10.0, \"CSV\", null";

        PersistentResource resource = mock(PersistentResource.class);
        when(resource.getObject()).thenReturn(queryObj);
        String output = formatter.format(resource, 1);
        assertEquals(true, output.contains(row));
    }

    @Test
    public void testHeader() {
        CSVExportFormatter formatter = new CSVExportFormatter(elide, false);

        TableExport queryObj = new TableExport();
        String query = "{ tableExport { edges { node { query queryType } } } }";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.GRAPHQL_V1_0);
        queryObj.setResultType(ResultType.CSV);

        // Prepare EntityProjection
        Set<Attribute> attributes = new LinkedHashSet<Attribute>();
        attributes.add(Attribute.builder().type(TableExport.class).name("query").alias("query").build());
        attributes.add(Attribute.builder().type(TableExport.class).name("queryType").build());
        EntityProjection projection = EntityProjection.builder().type(TableExport.class).attributes(attributes).build();

        String output = formatter.preFormat(projection, queryObj);
        assertEquals("\"query\",\"queryType\"", output);
    }

    @Test
    public void testHeaderSkip() {
        CSVExportFormatter formatter = new CSVExportFormatter(elide, true);

        TableExport queryObj = new TableExport();
        String query = "{ tableExport { edges { node { query queryType } } } }";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.GRAPHQL_V1_0);
        queryObj.setResultType(ResultType.CSV);

        // Prepare EntityProjection
        Set<Attribute> attributes = new LinkedHashSet<Attribute>();
        attributes.add(Attribute.builder().type(TableExport.class).name("query").alias("query").build());
        attributes.add(Attribute.builder().type(TableExport.class).name("queryType").build());
        EntityProjection projection = EntityProjection.builder().type(TableExport.class).attributes(attributes).build();

        String output = formatter.preFormat(projection, queryObj);
        assertEquals(null, output);
    }
}
