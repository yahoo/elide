/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.export.formatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yahoo.elide.async.models.QueryType;
import com.yahoo.elide.async.models.ResultType;
import com.yahoo.elide.async.models.TableExport;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.request.Attribute;
import com.yahoo.elide.core.request.EntityProjection;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

public class CSVExportFormatterTest {

    @Test
    public void testResourceToCSV() {
        CSVExportFormatter formatter = new CSVExportFormatter(false);

        String row2 = "\"edc4a871-dff2-4054-804e-d80075cf827d\", \"{ tableExport { edges { node { id query queryType requestId"
                + " principalName status createdOn updatedOn asyncAfterSeconds resultType result} } } }\", \"GRAPHQL_V1_0\"";
        String row2End = "10.0, \"CSV\", null";
        TableExport queryObj = new TableExport();
        String query = "{ tableExport { edges { node { id query queryType requestId principalName status createdOn updatedOn"
                + " asyncAfterSeconds resultType result} } } }";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.GRAPHQL_V1_0);
        queryObj.setResultType(ResultType.CSV);

        PersistentResource resource = mock(PersistentResource.class);
        when(resource.getObject()).thenReturn(queryObj);
        String output = formatter.format(resource, 1);
        assertEquals(true, output.contains(row2));
        assertEquals(true, output.endsWith(row2End));
    }

    @Test
    public void testHeader() {
        CSVExportFormatter formatter = new CSVExportFormatter(false);

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
}
