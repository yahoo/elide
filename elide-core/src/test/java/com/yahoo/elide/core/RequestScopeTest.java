/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import org.testng.Assert;
import org.testng.annotations.Test;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.lang.reflect.Method;
import java.util.Collections;

public class RequestScopeTest {
    @Test
    public void testFilterQueryParams() throws Exception {
        Method method = RequestScope.class.getDeclaredMethod("getFilterParams", MultivaluedMap.class);
        method.setAccessible(true);

        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.put("foo", Collections.singletonList("bar"));
        queryParams.put("filter", Collections.singletonList("baz"));
        queryParams.put("filter[xyz]", Collections.singletonList("buzz"));
        queryParams.put("bar", Collections.singletonList("foo"));

        MultivaluedMap<String, String> filtered = (MultivaluedMap<String, String>) method.invoke(null, queryParams);
        Assert.assertEquals(filtered.size(), 2);
        Assert.assertTrue(filtered.containsKey("filter"));
        Assert.assertTrue(filtered.containsKey("filter[xyz]"));
    }
}
