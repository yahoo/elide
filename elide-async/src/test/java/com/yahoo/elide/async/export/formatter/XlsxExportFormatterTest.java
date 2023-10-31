/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.export.formatter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettings;
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
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Consumer;

public class XlsxExportFormatterTest {
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

    public byte[] byteValueOf(Consumer<OutputStream> sink) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            sink.accept(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public byte[] format(TableExportFormatter formatter,  EntityProjection entityProjection,
            TableExport tableExport, PersistentResource resource) {
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
        XlsxExportFormatter formatter = new XlsxExportFormatter(true);
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

        Resource resource = new Resource("tableExport", "0", null, resourceAttributes, null, null, null);

        PersistentResource persistentResource = mock(PersistentResource.class);
        when(persistentResource.getObject()).thenReturn(queryObj);
        when(persistentResource.getRequestScope()).thenReturn(scope);
        when(persistentResource.toResource(any(), any())).thenReturn(resource);
        when(persistentResource.getAttribute((Attribute) any())).thenAnswer(key -> {
            return resourceAttributes.get(((Attribute) key.getArgument(0)).getName());
        });
        when(scope.getEntityProjection()).thenReturn(projection);

        byte[] output = format(formatter, projection, null, persistentResource);
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(output)) {
            XSSFWorkbook wb = new XSSFWorkbook(inputStream);
            XSSFSheet sheet = wb.getSheetAt(0);
            Object[] row1 = readRow(sheet, 0);
            assertArrayEquals(new Object[] {"query", "queryType", "createdOn"}, row1);
            Object[] row2 = readRow(sheet, 1);
            assertArrayEquals(new Object[] { query, queryObj.getQueryType().name(), queryObj.getCreatedOn() }, row2);
        }
    }

    protected Object[] readRow(XSSFSheet sheet, int rowNumber) {
        XSSFRow row = sheet.getRow(rowNumber);
        short start = row.getFirstCellNum();
        short end = row.getLastCellNum();
        Object[] result = new Object[end];
        for (short colIx = start; colIx < end; colIx++) {
            XSSFCell cell = row.getCell(colIx);
            if (cell == null) {
                continue;
            } else if (CellType.NUMERIC.equals(cell.getCellType())) {
                if (DateUtil.isCellDateFormatted(cell)) {
                    result[colIx] = cell.getDateCellValue();
                } else {
                    result[colIx] = cell.getNumericCellValue();
                }
            } else {
                result[colIx] = cell.getStringCellValue();
            }
        }
        return result;
    }

    @Test
    public void testNullResourceToXLSX() {
        XlsxExportFormatter formatter = new XlsxExportFormatter(true);
        PersistentResource persistentResource = null;
        byte[] output = format(formatter, null, null, persistentResource);
        assertEquals("", output);
    }

    @Test
    public void testNullProjectionHeader() {
        XlsxExportFormatter formatter = new XlsxExportFormatter(true);

        TableExport queryObj = new TableExport();

        // Prepare EntityProjection
        EntityProjection projection = null;

        byte[] output = format(formatter, projection, queryObj, null);
        assertEquals("", output);
    }

    @Test
    public void testProjectionWithEmptyAttributeSetHeader() {
        XlsxExportFormatter formatter = new XlsxExportFormatter(true);

        TableExport queryObj = new TableExport();

        // Prepare EntityProjection
        Set<Attribute> attributes = new LinkedHashSet<>();
        EntityProjection projection = EntityProjection.builder().type(TableExport.class).attributes(attributes).build();

        byte[] output = format(formatter, projection, queryObj, null);
        assertEquals("", output);
    }

    @Test
    public void testProjectionWithNullAttributesHeader() {
        XlsxExportFormatter formatter = new XlsxExportFormatter(true);

        TableExport queryObj = new TableExport();

        // Prepare EntityProjection
        Set<Attribute> attributes = null;
        EntityProjection projection = EntityProjection.builder().type(TableExport.class).attributes(attributes).build();

        byte[] output = format(formatter, projection, queryObj, null);
        assertEquals("", output);
    }

    @Test
    public void testHeader() {
        XlsxExportFormatter formatter = new XlsxExportFormatter(true);

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

        byte[] output = format(formatter, projection, queryObj, null);
        assertEquals("\"query\",\"queryType\"\r\n", output);
    }

    @Test
    public void testHeaderWithNonmatchingAlias() {
        XlsxExportFormatter formatter = new XlsxExportFormatter(true);

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

        byte[] output = format(formatter, projection, queryObj, null);
        assertEquals("\"query\",\"queryType\"\r\n", output);
    }

    @Test
    public void testHeaderWithArguments() {
        XlsxExportFormatter formatter = new XlsxExportFormatter(true);

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

        byte[] output = format(formatter, projection, queryObj, null);
        assertEquals("\"query(foo=bar)\",\"queryType(foo=bar baz=boo)\"\r\n", output);
    }

    @Test
    public void testHeaderSkip() {
        XlsxExportFormatter formatter = new XlsxExportFormatter(false);

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

        byte[] output = format(formatter, projection, queryObj, null);
        assertEquals("", output);
    }
}
