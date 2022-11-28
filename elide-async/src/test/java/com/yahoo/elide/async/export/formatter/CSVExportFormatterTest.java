/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.export.formatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.async.models.QueryType;
import com.yahoo.elide.async.models.ResultType;
import com.yahoo.elide.async.models.TableExport;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.core.request.Attribute;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.security.checks.Check;
import com.yahoo.elide.core.utils.DefaultClassScanner;
import com.yahoo.elide.jsonapi.models.Resource;
import org.apache.commons.lang3.time.FastDateFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

public class CSVExportFormatterTest {
    public static final String FORMAT = "yyyy-MM-dd'T'HH:mm'Z'";
    private static final FastDateFormat FORMATTER = FastDateFormat.getInstance(FORMAT, TimeZone.getTimeZone("GMT"));
    private HashMapDataStore dataStore;
    private Elide elide;
    private RequestScope scope;

    @BeforeEach
    public void setupMocks(@TempDir Path tempDir) {
        dataStore = new HashMapDataStore(DefaultClassScanner.getInstance(), TableExport.class.getPackage());
        Map<String, Class<? extends Check>> map = new HashMap<>();
        elide = new Elide(
                    new ElideSettingsBuilder(dataStore)
                        .withEntityDictionary(EntityDictionary.builder().checks(map).build())
                        .withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"))
                        .build());
        elide.doScans();
        scope = mock(RequestScope.class);
    }

    @Test
    public void testResourceToCSV() {
        CSVExportFormatter formatter = new CSVExportFormatter(elide, false);
        TableExport queryObj = new TableExport();
        String query = "{ tableExport { edges { node { query queryType createdOn} } } }";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.GRAPHQL_V1_0);
        queryObj.setResultType(ResultType.CSV);

        String row = "\"{ tableExport { edges { node { query queryType createdOn} } } }\", \"GRAPHQL_V1_0\""
                + ", \"" + FORMATTER.format(queryObj.getCreatedOn());

        // Prepare EntityProjection
        Set<Attribute> attributes = new LinkedHashSet<>();
        attributes.add(Attribute.builder().type(TableExport.class).name("query").alias("query").build());
        attributes.add(Attribute.builder().type(TableExport.class).name("queryType").build());
        attributes.add(Attribute.builder().type(TableExport.class).name("createdOn").build());
        EntityProjection projection = EntityProjection.builder().type(TableExport.class).attributes(attributes).build();

        Map<String, Object> resourceAttributes = new LinkedHashMap<>();
        resourceAttributes.put("query", query);
        resourceAttributes.put("queryType", queryObj.getQueryType());
        resourceAttributes.put("createdOn", queryObj.getCreatedOn());

        Resource resource = new Resource("tableExport", "0", resourceAttributes, null, null, null);

        PersistentResource persistentResource = mock(PersistentResource.class);
        when(persistentResource.getObject()).thenReturn(queryObj);
        when(persistentResource.getRequestScope()).thenReturn(scope);
        when(persistentResource.toResource(any(), any())).thenReturn(resource);
        when(scope.getEntityProjection()).thenReturn(projection);

        String output = formatter.format(persistentResource, 1);
        assertTrue(output.contains(row));
    }

    @Test
    public void testNullResourceToCSV() {
        CSVExportFormatter formatter = new CSVExportFormatter(elide, false);
        PersistentResource persistentResource = null;

        String output = formatter.format(persistentResource, 1);
        assertNull(output);
    }

    @Test
    public void testNullProjectionHeader() {
        CSVExportFormatter formatter = new CSVExportFormatter(elide, false);

        TableExport queryObj = new TableExport();

        // Prepare EntityProjection
        EntityProjection projection = null;

        String output = formatter.preFormat(projection, queryObj);
        assertNull(output);
    }

    @Test
    public void testProjectionWithEmptyAttributeSetHeader() {
        CSVExportFormatter formatter = new CSVExportFormatter(elide, false);

        TableExport queryObj = new TableExport();

        // Prepare EntityProjection
        Set<Attribute> attributes = new LinkedHashSet<>();
        EntityProjection projection = EntityProjection.builder().type(TableExport.class).attributes(attributes).build();

        String output = formatter.preFormat(projection, queryObj);
        assertEquals("", output);
    }

    @Test
    public void testProjectionWithNullAttributesHeader() {
        CSVExportFormatter formatter = new CSVExportFormatter(elide, false);

        TableExport queryObj = new TableExport();

        // Prepare EntityProjection
        Set<Attribute> attributes = null;
        EntityProjection projection = EntityProjection.builder().type(TableExport.class).attributes(attributes).build();

        String output = formatter.preFormat(projection, queryObj);
        assertEquals("", output);
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
        Set<Attribute> attributes = new LinkedHashSet<>();
        attributes.add(Attribute.builder().type(TableExport.class).name("query").alias("query").build());
        attributes.add(Attribute.builder().type(TableExport.class).name("queryType").build());
        EntityProjection projection = EntityProjection.builder().type(TableExport.class).attributes(attributes).build();

        String output = formatter.preFormat(projection, queryObj);
        assertEquals("\"query\",\"queryType\"", output);
    }

    @Test
    public void testHeaderWithNonmatchingAlias() {
        CSVExportFormatter formatter = new CSVExportFormatter(elide, false);

        TableExport queryObj = new TableExport();
        String query = "{ tableExport { edges { node { query queryType } } } }";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.GRAPHQL_V1_0);
        queryObj.setResultType(ResultType.CSV);

        // Prepare EntityProjection
        Set<Attribute> attributes = new LinkedHashSet<>();
        attributes.add(Attribute.builder().type(TableExport.class).name("query").alias("foo").build());
        attributes.add(Attribute.builder().type(TableExport.class).name("queryType").build());
        EntityProjection projection = EntityProjection.builder().type(TableExport.class).attributes(attributes).build();

        String output = formatter.preFormat(projection, queryObj);
        assertEquals("\"query\",\"queryType\"", output);
    }

    @Test
    public void testHeaderWithArguments() {
        CSVExportFormatter formatter = new CSVExportFormatter(elide, false);

        TableExport queryObj = new TableExport();
        String query = "{ tableExport { edges { node { query queryType } } } }";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.GRAPHQL_V1_0);
        queryObj.setResultType(ResultType.CSV);

        // Prepare EntityProjection
        Set<Attribute> attributes = new LinkedHashSet<>();
        attributes.add(Attribute.builder()
                .type(TableExport.class)
                .name("query")
                .argument(Argument.builder().name("foo").value("bar").build())
                .alias("query").build());

        attributes.add(Attribute.builder()
                .type(TableExport.class)
                .argument(Argument.builder().name("foo").value("bar").build())
                .argument(Argument.builder().name("baz").value("boo").build())
                .name("queryType")
                .build());
        EntityProjection projection = EntityProjection.builder().type(TableExport.class).attributes(attributes).build();

        String output = formatter.preFormat(projection, queryObj);
        assertEquals("\"query(foo=bar)\",\"queryType(foo=bar baz=boo)\"", output);
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
        Set<Attribute> attributes = new LinkedHashSet<>();
        attributes.add(Attribute.builder().type(TableExport.class).name("query").alias("query").build());
        attributes.add(Attribute.builder().type(TableExport.class).name("queryType").build());
        EntityProjection projection = EntityProjection.builder().type(TableExport.class).attributes(attributes).build();

        String output = formatter.preFormat(projection, queryObj);
        assertNull(output);
    }
}
