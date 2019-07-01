/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.search.constraints;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.datastores.search.models.Item;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;

public class NGramConstraintTest {
    private RSQLFilterDialect filterParser;
    private IndexedFieldConstraint constraint;

    public NGramConstraintTest() {
        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(Item.class);
        filterParser = new RSQLFilterDialect(dictionary);
        constraint = new NGramConstraint(3, 5, dictionary);
    }

    @Test(expectedExceptions = InvalidValueException.class)
    public void testNgramTooSmall() throws Exception {
        FilterPredicate filter = (FilterPredicate) filterParser.parseFilterExpression("description==*ru*",
                Item.class, false);

        constraint.canSearch(Item.class, filter);
    }

    @Test(expectedExceptions = InvalidValueException.class)
    public void testNgramTooLarge() throws Exception {
        FilterPredicate filter = (FilterPredicate) filterParser.parseFilterExpression("description==*ruabcd*",
                Item.class, false);

        constraint.canSearch(Item.class, filter);
    }

    @Test
    public void testNgramJustRight() throws Exception {
        FilterPredicate filter = (FilterPredicate) filterParser.parseFilterExpression("description==*ruabc*",
                Item.class, false);

        Assert.assertEquals(constraint.canSearch(Item.class, filter), DataStoreTransaction.FeatureSupport.FULL);
    }
}
