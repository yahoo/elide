/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

import org.testng.Assert;
import org.testng.annotations.Test;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

/**
 * Predicate test class.
 */
public class PredicateTest {
    @Test
    public void testParseQueryParams() throws Exception {
        final MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("filter[collection1.field1]", "abc");
        queryParams.add("filter[collection1.field2]", "def");
        queryParams.add("filter[collection2.field1]", "ghi,jkl");
        queryParams.add("filter[collection3.nested1.field1][equals]", "jkl,mno");
        queryParams.add("filter[collection3.nested2.field2][in]", "pqr , stu");

        final Set<Predicate> predicateSet = Predicate.parseQueryParams(queryParams);
        predicateSet.forEach(predicate -> {
            switch (predicate.getKey()) {
                case "collection1.field1":
                    Assert.assertEquals(predicate.getField(), "field1");
                    Assert.assertEquals(predicate.getCollection(), "collection1");
                    Assert.assertEquals(predicate.getLineage(), Collections.singletonList("collection1"));
                    Assert.assertEquals(predicate.getOperator(), Operator.IN);
                    Assert.assertEquals(predicate.getValues(), new String[]{"abc"});
                    break;
                case "collection1.field2":
                    Assert.assertEquals(predicate.getField(), "field2");
                    Assert.assertEquals(predicate.getCollection(), "collection1");
                    Assert.assertEquals(predicate.getLineage(), Collections.singletonList("collection1"));
                    Assert.assertEquals(predicate.getOperator(), Operator.IN);
                    Assert.assertEquals(predicate.getValues(), new String[]{"def"});
                    break;
                case "collection2.field1":
                    Assert.assertEquals(predicate.getField(), "field1");
                    Assert.assertEquals(predicate.getCollection(), "collection2");
                    Assert.assertEquals(predicate.getLineage(), Collections.singletonList("collection2"));
                    Assert.assertEquals(predicate.getOperator(), Operator.IN);
                    Assert.assertEquals(predicate.getValues(), new String[]{"ghi", "jkl"});
                    break;
                case "collection3.nested1.field1":
                    Assert.assertEquals(predicate.getField(), "field1");
                    Assert.assertEquals(predicate.getCollection(), "nested1");
//                    Assert.assertEquals(predicate.getLineage(), "collection3.nested1");
                    Assert.assertEquals(predicate.getLineage(), Arrays.asList("collection3", "nested1"));
                    Assert.assertEquals(predicate.getOperator(), Operator.EQUALS);
                    Assert.assertEquals(predicate.getValues(), new String[]{"jkl", "mno"});
                    break;
                case "collection3.nested2.field2":
                    Assert.assertEquals(predicate.getField(), "field2");
                    Assert.assertEquals(predicate.getCollection(), "nested2");
                    Assert.assertEquals(predicate.getLineage(), Arrays.asList("collection3", "nested2"));
                    Assert.assertEquals(predicate.getOperator(), Operator.IN);
                    Assert.assertEquals(predicate.getValues(), new String[]{"pqr", "stu"});
                    break;
                default:
                    Assert.fail(predicate.toString() + " case not covered");
            }
        });
    }
}
