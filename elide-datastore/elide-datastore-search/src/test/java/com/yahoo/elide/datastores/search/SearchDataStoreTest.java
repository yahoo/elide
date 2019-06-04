/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.search;


import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.datastores.search.models.Item;
import org.testng.annotations.Test;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.HashMap;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SearchDataStoreTest {

    private EntityDictionary dictionary;
    private RSQLFilterDialect filterParser;

    public SearchDataStoreTest() {
        dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(Item.class);

        filterParser = new RSQLFilterDialect(dictionary);
    }

    @Test
    public void someTest() throws Exception {

        DataStore mockStore = mock(DataStore.class);

        DataStoreTransaction mockTransaction = mock(DataStoreTransaction.class);
        when(mockStore.beginReadTransaction()).thenReturn(mockTransaction);

        RequestScope mockScope = mock(RequestScope.class);

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("searchDataStoreTest");

        SearchDataStore searchStore = new SearchDataStore(mockStore, emf);

        searchStore.populateEntityDictionary(dictionary);

        DataStoreTransaction testTransaction = searchStore.beginReadTransaction();

        FilterExpression filter = filterParser.parseFilterExpression("name==Drum", Item.class);

        Iterable<Object> loaded = testTransaction.loadObjects(Item.class, Optional.of(filter), Optional.empty(), Optional.empty(), mockScope);

        System.out.println(loaded);
    }
}
