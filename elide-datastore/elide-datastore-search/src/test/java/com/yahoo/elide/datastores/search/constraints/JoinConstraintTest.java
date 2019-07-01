/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.search.constraints;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.datastores.search.models.Item;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

public class JoinConstraintTest {

    private RSQLFilterDialect filterParser;
    private JoinConstraint constraint;

    @Entity
    @Include
    class Book {
        @Id
        private long id;

        @ManyToMany
        private Set<Author>  authors;
    }

    @Entity
    @Include
    class Author {
        @Id
        private long id;

        private String name;

        @ManyToMany
        private Set<Book> books;
    }

    public JoinConstraintTest() {
        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(Item.class);
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Book.class);
        filterParser = new RSQLFilterDialect(dictionary);
        constraint = new JoinConstraint();
    }

    @Test
    public void testInvalidJoin() throws Exception {
        FilterPredicate filter = (FilterPredicate) filterParser.parseFilterExpression("authors.name==*abc*",
                Book.class, true);

        Assert.assertEquals(constraint.canSearch(Item.class, filter), DataStoreTransaction.FeatureSupport.NONE);
    }

    @Test
    public void testNoJoin() throws Exception {
        FilterPredicate filter = (FilterPredicate) filterParser.parseFilterExpression("name==*abc*",
                Author.class, true);

        Assert.assertEquals(constraint.canSearch(Item.class, filter), DataStoreTransaction.FeatureSupport.FULL);
    }
}
