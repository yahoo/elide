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

import org.junit.jupiter.api.Test;

public class JSONExportFormatterTest {

    @Test
    public void testResourceToJSON() {
        JSONExportFormatter formatter = new JSONExportFormatter();

        String start = "{\"id\":\"edc4a871-dff2-4054-804e-d80075cf827d\",\"query\":\"/tableExport\","
                + "\"queryType\":\"GRAPHQL_V1_0\",";
        String end = "\"asyncAfterSeconds\":10,\"resultType\":\"CSV\",\"result\":null}";
        TableExport queryObj = new TableExport();
        String query = "/tableExport";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.GRAPHQL_V1_0);
        queryObj.setResultType(ResultType.CSV);

        PersistentResource resource = mock(PersistentResource.class);
        when(resource.getObject()).thenReturn(queryObj);
        String output = formatter.format(resource, 1);
        assertEquals(true, output.startsWith(start));
        assertEquals(true, output.endsWith(end));

    }
}
