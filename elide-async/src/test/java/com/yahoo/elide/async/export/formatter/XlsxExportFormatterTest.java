/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.export.formatter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.paiondata.elide.Elide;
import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.async.models.Export;
import com.paiondata.elide.async.models.QueryType;
import com.paiondata.elide.async.models.ResultType;
import com.paiondata.elide.async.models.TableExport;
import com.paiondata.elide.core.PersistentResource;
import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.datastore.inmemory.HashMapDataStore;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.request.Argument;
import com.paiondata.elide.core.request.Attribute;
import com.paiondata.elide.core.request.EntityProjection;
import com.paiondata.elide.core.security.checks.Check;
import com.paiondata.elide.core.utils.DefaultClassScanner;
import com.paiondata.elide.jsonapi.models.Resource;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Consumer;

public class XlsxExportFormatterTest {
    public static final String FORMAT = "yyyy-MM-dd'T'HH:mm'Z'";
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

    public byte[] byteValueOf(Consumer<OutputStream> sink) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            sink.accept(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public byte[] format(TableExportFormatter formatter, EntityProjection entityProjection,
            TableExport tableExport, PersistentResource<?> resource) {
        return byteValueOf(outputStream -> {
            try (ResourceWriter writer = formatter.newResourceWriter(outputStream, entityProjection, tableExport)) {
                if (resource != null) {
                    writer.write(resource);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @Test
    public void testResourceToXLSX() throws IOException {
        XlsxExportFormatter formatter = new XlsxExportFormatter(elide, true);
        TableExport queryObj = new TableExport();
        String query = "{ tableExport { edges { node { query queryType createdOn} } } }";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.GRAPHQL_V1_0);
        queryObj.setResultType(ResultType.XLSX);

        // Prepare EntityProjection
        Set<Attribute> attributes = new LinkedHashSet<>();
        attributes.add(Attribute.builder().type(String.class).name("query").alias("query").build());
        attributes.add(Attribute.builder().type(QueryType.class).name("queryType").build());
        attributes.add(Attribute.builder().type(Date.class).name("createdOn").build());
        EntityProjection projection = EntityProjection.builder().type(TableExport.class).attributes(attributes).build();

        Map<String, Object> resourceAttributes = new LinkedHashMap<>();
        resourceAttributes.put("query", query);
        resourceAttributes.put("queryType", queryObj.getQueryType());
        resourceAttributes.put("createdOn", queryObj.getCreatedOn());

        Resource resource = new Resource("tableExport", "0", null, resourceAttributes, null, null, null);

        PersistentResource persistentResource = mock(PersistentResource.class);
        when(persistentResource.getObject()).thenReturn(queryObj);
        when(persistentResource.getRequestScope()).thenReturn(scope);
        when(persistentResource.getAttribute(any(Attribute.class))).thenAnswer(key -> {
            return resourceAttributes.get(((Attribute) key.getArgument(0)).getName());
        });
        when(scope.getEntityProjection()).thenReturn(projection);

        byte[] output = format(formatter, projection, null, persistentResource);
        List<Object[]> results = XlsxTestUtils.read(output);
        assertArrayEquals(new Object[] { "query", "queryType", "createdOn" }, results.get(0));
        assertArrayEquals(new Object[] { query, queryObj.getQueryType().name(), queryObj.getCreatedOn() }, results.get(1));
    }

    protected boolean isEmpty(XSSFSheet sheet) {
        return sheet.getFirstRowNum() == -1;
    }

    @Test
    public void testNullResourceToXLSX() throws IOException {
        XlsxExportFormatter formatter = new XlsxExportFormatter(elide, true);
        PersistentResource persistentResource = null;
        byte[] output = format(formatter, null, null, persistentResource);
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(output);
                XSSFWorkbook wb = new XSSFWorkbook(inputStream)) {
            XSSFSheet sheet = wb.getSheetAt(0);
            assertTrue(isEmpty(sheet));
        }
    }

    @Test
    public void testNullProjectionHeader() throws IOException {
        XlsxExportFormatter formatter = new XlsxExportFormatter(elide, true);

        TableExport queryObj = new TableExport();

        // Prepare EntityProjection
        EntityProjection projection = null;

        byte[] output = format(formatter, projection, queryObj, null);
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(output);
                XSSFWorkbook wb = new XSSFWorkbook(inputStream)) {
            XSSFSheet sheet = wb.getSheetAt(0);
            assertTrue(isEmpty(sheet));
        }
    }

    @Test
    public void testProjectionWithEmptyAttributeSetHeader() throws IOException {
        XlsxExportFormatter formatter = new XlsxExportFormatter(elide, true);

        TableExport queryObj = new TableExport();

        // Prepare EntityProjection
        Set<Attribute> attributes = new LinkedHashSet<>();
        EntityProjection projection = EntityProjection.builder().type(TableExport.class).attributes(attributes).build();

        byte[] output = format(formatter, projection, queryObj, null);
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(output);
                XSSFWorkbook wb = new XSSFWorkbook(inputStream)) {
            XSSFSheet sheet = wb.getSheetAt(0);
            assertTrue(isEmpty(sheet));
        }
    }

    @Test
    public void testProjectionWithNullAttributesHeader() throws IOException {
        XlsxExportFormatter formatter = new XlsxExportFormatter(elide, true);

        TableExport queryObj = new TableExport();

        // Prepare EntityProjection
        Set<Attribute> attributes = null;
        EntityProjection projection = EntityProjection.builder().type(TableExport.class).attributes(attributes).build();

        byte[] output = format(formatter, projection, queryObj, null);
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(output);
                XSSFWorkbook wb = new XSSFWorkbook(inputStream)) {
            XSSFSheet sheet = wb.getSheetAt(0);
            assertTrue(isEmpty(sheet));
        }
    }

    @Test
    public void testHeader() throws IOException {
        XlsxExportFormatter formatter = new XlsxExportFormatter(elide, true);

        TableExport queryObj = new TableExport();
        String query = "{ tableExport { edges { node { query queryType } } } }";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.GRAPHQL_V1_0);
        queryObj.setResultType(ResultType.XLSX);

        // Prepare EntityProjection
        Set<Attribute> attributes = new LinkedHashSet<>();
        attributes.add(Attribute.builder().type(String.class).name("query").alias("query").build());
        attributes.add(Attribute.builder().type(QueryType.class).name("queryType").build());
        EntityProjection projection = EntityProjection.builder().type(TableExport.class).attributes(attributes).build();

        byte[] output = format(formatter, projection, queryObj, null);
        List<Object[]> results = XlsxTestUtils.read(output);
        assertArrayEquals(new Object[] { "query", "queryType"}, results.get(0));
    }

    @Test
    public void testHeaderWithNonmatchingAlias() throws IOException {
        XlsxExportFormatter formatter = new XlsxExportFormatter(elide, true);

        TableExport queryObj = new TableExport();
        String query = "{ tableExport { edges { node { query queryType } } } }";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.GRAPHQL_V1_0);
        queryObj.setResultType(ResultType.XLSX);

        // Prepare EntityProjection
        Set<Attribute> attributes = new LinkedHashSet<>();
        attributes.add(Attribute.builder().type(String.class).name("query").alias("foo").build());
        attributes.add(Attribute.builder().type(QueryType.class).name("queryType").build());
        EntityProjection projection = EntityProjection.builder().type(TableExport.class).attributes(attributes).build();

        byte[] output = format(formatter, projection, queryObj, null);
        List<Object[]> results = XlsxTestUtils.read(output);
        assertArrayEquals(new Object[] { "foo", "queryType"}, results.get(0));
    }

    @Test
    public void testHeaderWithArguments() throws IOException {
        XlsxExportFormatter formatter = new XlsxExportFormatter(elide, true);

        TableExport queryObj = new TableExport();
        String query = "{ tableExport { edges { node { query queryType } } } }";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.GRAPHQL_V1_0);
        queryObj.setResultType(ResultType.XLSX);

        // Prepare EntityProjection
        Set<Attribute> attributes = new LinkedHashSet<>();
        attributes.add(Attribute.builder()
                .type(String.class)
                .name("query")
                .argument(Argument.builder().name("foo").value("bar").build())
                .alias("query").build());

        attributes.add(Attribute.builder()
                .type(QueryType.class)
                .argument(Argument.builder().name("foo").value("bar").build())
                .argument(Argument.builder().name("baz").value("boo").build())
                .name("queryType")
                .build());
        EntityProjection projection = EntityProjection.builder().type(TableExport.class).attributes(attributes).build();

        byte[] output = format(formatter, projection, queryObj, null);
        List<Object[]> results = XlsxTestUtils.read(output);
        assertArrayEquals(new Object[] { "query(foo=bar)", "queryType(foo=bar baz=boo)"}, results.get(0));
    }

    @Test
    public void testHeaderSkip() throws IOException {
        XlsxExportFormatter formatter = new XlsxExportFormatter(elide, false);

        TableExport queryObj = new TableExport();
        String query = "{ tableExport { edges { node { query queryType } } } }";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.GRAPHQL_V1_0);
        queryObj.setResultType(ResultType.XLSX);

        // Prepare EntityProjection
        Set<Attribute> attributes = new LinkedHashSet<>();
        attributes.add(Attribute.builder().type(String.class).name("query").alias("query").build());
        attributes.add(Attribute.builder().type(QueryType.class).name("queryType").build());
        EntityProjection projection = EntityProjection.builder().type(TableExport.class).attributes(attributes).build();

        byte[] output = format(formatter, projection, queryObj, null);
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(output);
                XSSFWorkbook wb = new XSSFWorkbook(inputStream)) {
            XSSFSheet sheet = wb.getSheetAt(0);
            assertTrue(isEmpty(sheet));
        }
    }

    @Test
    public void testComplexResourceToXLSX() throws IOException {
        XlsxExportFormatter formatter = new XlsxExportFormatter(elide, true);
        Export export = new Export();
        export.setName("name");
        export.setAlternatives(Set.of("a", "b", "c"));

        // Prepare EntityProjection
        Set<Attribute> attributes = new LinkedHashSet<>();
        attributes.add(Attribute.builder().type(String.class).name("name").build());
        attributes.add(Attribute.builder().type(Set.class).name("alternatives").build());
        EntityProjection projection = EntityProjection.builder().type(Export.class).attributes(attributes).build();

        Map<String, Object> resourceAttributes = new LinkedHashMap<>();
        resourceAttributes.put("name", export.getName());
        resourceAttributes.put("alternatives", export.getAlternatives());

        Resource resource = new Resource("export", "0", null, resourceAttributes, null, null, null);

        PersistentResource persistentResource = mock(PersistentResource.class);
        when(persistentResource.getObject()).thenReturn(export);
        when(persistentResource.getRequestScope()).thenReturn(scope);
        when(persistentResource.getAttribute(any(Attribute.class))).thenAnswer(key -> {
            return resourceAttributes.get(((Attribute) key.getArgument(0)).getName());
        });
        when(scope.getEntityProjection()).thenReturn(projection);

        byte[] output = format(formatter, projection, null, persistentResource);
        List<Object[]> results = XlsxTestUtils.read(output);
        assertArrayEquals(new Object[] { "name", "alternatives"}, results.get(0));
        assertArrayEquals(
                new Object[] { export.getName(), String.join(";", export.getAlternatives()) },
                results.get(1));
    }
}
