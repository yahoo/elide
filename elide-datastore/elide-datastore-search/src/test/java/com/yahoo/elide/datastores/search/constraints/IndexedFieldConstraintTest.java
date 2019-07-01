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

public class IndexedFieldConstraintTest {
    private RSQLFilterDialect filterParser;
    private IndexedFieldConstraint constraint;

    public IndexedFieldConstraintTest() {
        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(Item.class);
        filterParser = new RSQLFilterDialect(dictionary);
        constraint = new IndexedFieldConstraint(dictionary);
    }

    @Test
    public void testIndexedFields() throws Exception {
        FilterPredicate filter = (FilterPredicate) filterParser.parseFilterExpression("name==*rum*",
                Item.class, false);

        Assert.assertEquals(constraint.canSearch(Item.class, filter), DataStoreTransaction.FeatureSupport.FULL);
    }

    @Test
    public void testIndexedField() throws Exception {
        FilterPredicate filter = (FilterPredicate) filterParser.parseFilterExpression("description==*rum*",
                Item.class, false);

        Assert.assertEquals(constraint.canSearch(Item.class, filter), DataStoreTransaction.FeatureSupport.FULL);
    }

    @Test
    public void testUnindexedField() throws Exception {
        FilterPredicate filter = (FilterPredicate) filterParser.parseFilterExpression("price==123",
                Item.class, false);

        Assert.assertEquals(constraint.canSearch(Item.class, filter), DataStoreTransaction.FeatureSupport.NONE);
    }
}
