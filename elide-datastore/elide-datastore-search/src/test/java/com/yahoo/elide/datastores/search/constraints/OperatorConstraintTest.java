/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.search.constraints;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.datastores.search.models.Item;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;

public class OperatorConstraintTest {

    private RSQLFilterDialect filterParser;
    private OperatorConstraint constraint = new OperatorConstraint(DataStoreTransaction.FeatureSupport.FULL,
                DataStoreTransaction.FeatureSupport.PARTIAL);


    public OperatorConstraintTest() {
        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(Item.class);
        filterParser = new RSQLFilterDialect(dictionary);
    }

    @Test
    public void testInfix() throws Exception {
        FilterPredicate filter = (FilterPredicate) filterParser.parseFilterExpression("name==*rum*",
                Item.class, false);

        Assert.assertEquals(constraint.canSearch(Item.class, filter), DataStoreTransaction.FeatureSupport.FULL);
    }

    @Test
    public void testPrefix() throws Exception {
        FilterPredicate filter = (FilterPredicate) filterParser.parseFilterExpression("name==drum*",
                Item.class, false);

        Assert.assertEquals(constraint.canSearch(Item.class, filter), DataStoreTransaction.FeatureSupport.PARTIAL);
    }

    @Test
    public void testEquals() throws Exception {
        FilterPredicate filter = (FilterPredicate) filterParser.parseFilterExpression("name==drum",
                Item.class, false);

        Assert.assertEquals(constraint.canSearch(Item.class, filter), DataStoreTransaction.FeatureSupport.NONE);
    }
}
