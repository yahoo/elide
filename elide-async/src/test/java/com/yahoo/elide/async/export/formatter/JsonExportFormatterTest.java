/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.export.formatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.paiondata.elide.Elide;
import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.async.models.QueryType;
import com.paiondata.elide.async.models.ResultType;
import com.paiondata.elide.async.models.TableExport;
import com.paiondata.elide.core.PersistentResource;
import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.datastore.inmemory.HashMapDataStore;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.request.Attribute;
import com.paiondata.elide.core.request.EntityProjection;
import com.paiondata.elide.core.security.checks.Check;
import com.paiondata.elide.core.utils.DefaultClassScanner;
import com.paiondata.elide.jsonapi.models.Resource;
import org.apache.commons.lang3.time.FastDateFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Consumer;

public class JsonExportFormatterTest {
    public static final String FORMAT = "yyyy-MM-dd'T'HH:mm'Z'";
    private static final FastDateFormat FORMATTER = FastDateFormat.getInstance(FORMAT, TimeZone.getTimeZone("GMT"));
    private HashMapDataStore dataStore;
    private Elide elide;
    private RequestScope scope;

    @BeforeEach
    public void setupMocks(@TempDir Path tempDir) {
        dataStore = new HashMapDataStore(new DefaultClassScanner(), TableExport.class.getPackage());
        Map<String, Class<? extends Check>> map = new HashMap<>();
        elide = new Elide(
                ElideSettings.builder().dataStore(dataStore)
                        .entityDictionary(EntityDictionary.builder().checks(map).build())
                        .serdes(serdes -> serdes.withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC")))
                        .build());
        elide.doScans();
        scope = mock(RequestScope.class);
    }

    public String stringValueOf(Consumer<OutputStream> sink) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            sink.accept(outputStream);
            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String format(TableExportFormatter formatter,  EntityProjection entityProjection,
            TableExport tableExport, PersistentResource resource) {
        return stringValueOf(outputStream -> {
            try (ResourceWriter writer = formatter.newResourceWriter(outputStream, entityProjection, tableExport)) {
                writer.write(resource);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @Test
    public void testFormat() {
        JsonExportFormatter formatter = new JsonExportFormatter(elide);
        TableExport queryObj = new TableExport();
        String query = "{ tableExport { edges { node { query queryType createdOn} } } }";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.GRAPHQL_V1_0);
        queryObj.setResultType(ResultType.CSV);

        String start = "{\"query\":\"{ tableExport { edges { node { query queryType createdOn} } } }\","
                + "\"queryType\":\"GRAPHQL_V1_0\",\"createdOn\":\"" + FORMATTER.format(queryObj.getCreatedOn()) + "\"}";

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

        Resource resource = new Resource("tableExport", "0", null, resourceAttributes, null, null, null);
        PersistentResource persistentResource = mock(PersistentResource.class);
        when(persistentResource.getObject()).thenReturn(queryObj);
        when(persistentResource.getRequestScope()).thenReturn(scope);
        when(persistentResource.getAttribute(any(Attribute.class))).thenAnswer(invocation -> {
            Attribute attribute = invocation.getArgument(0);
            return resourceAttributes.get(attribute.getName());
        });
        when(scope.getEntityProjection()).thenReturn(projection);

        String output = format(formatter, null, null, persistentResource);
        assertTrue(output.contains(start));
    }

    @Test
    public void testResourceToJSON() throws IOException {
        JsonExportFormatter formatter = new JsonExportFormatter(elide);
        TableExport queryObj = new TableExport();
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);

        String start = "{\"query\":\"{ tableExport { edges { node { query queryType} } } }\","
                + "\"queryType\":\"GRAPHQL_V1_0\"}";

        // Prepare EntityProjection
        Set<Attribute> attributes = new LinkedHashSet<>();
        attributes.add(Attribute.builder().type(TableExport.class).name("query").alias("query").build());
        attributes.add(Attribute.builder().type(TableExport.class).name("queryType").build());
        EntityProjection projection = EntityProjection.builder().type(TableExport.class).attributes(attributes).build();

        Map<String, Object> resourceAttributes = new LinkedHashMap<>();
        resourceAttributes.put("query", "{ tableExport { edges { node { query queryType} } } }");
        resourceAttributes.put("queryType", QueryType.GRAPHQL_V1_0);

        Resource resource = new Resource("tableExport", "0", null, resourceAttributes, null, null, null);
        PersistentResource persistentResource = mock(PersistentResource.class);
        when(persistentResource.getObject()).thenReturn(queryObj);
        when(persistentResource.getRequestScope()).thenReturn(scope);
        when(persistentResource.getAttribute(any(Attribute.class))).thenAnswer(invocation -> {
            Attribute attribute = invocation.getArgument(0);
            return resourceAttributes.get(attribute.getName());
        });
        when(scope.getEntityProjection()).thenReturn(projection);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ResourceWriter writer = formatter.newResourceWriter(outputStream, projection, queryObj);
            writer.write(persistentResource);
            writer.close();
            String output = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
            assertTrue(output.contains(start));
        }
    }

    @Test
    public void testNullResourceToJSON() {
        JsonExportFormatter formatter = new JsonExportFormatter(elide);
        PersistentResource persistentResource = null;

        String output = format(formatter, null, null, persistentResource);
        assertEquals("[\n]\n", output);
    }
}
