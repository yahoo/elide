/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.jpa;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.annotations.JPQLFilterFragment;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.FilterTranslator;
import com.yahoo.elide.core.filter.JPQLPredicateGenerator;
import com.yahoo.elide.core.filter.Operator;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;

public class JpaDataStoreTest {
    public static class TestGenerator implements JPQLPredicateGenerator {
        @Override
        public String generate(String columnAlias, List<FilterPredicate.FilterParameter> parameters) {
            return "FOO()";
        }
    }

    @Test
    public void verifyJPQLGeneratorRegistration() {

        @Include
        @Entity
        class Test {
            @Id
            private long id;

            @JPQLFilterFragment(operator = Operator.IN, generator = TestGenerator.class)
            private String name;
        }

        EntityType mockType = mock(EntityType.class);
        when(mockType.getJavaType()).thenReturn(Test.class);

        Metamodel mockModel = mock(Metamodel.class);
        when(mockModel.getEntities()).thenReturn(Sets.newHashSet(mockType));

        EntityManager managerMock = mock(EntityManager.class);
        when(managerMock.getMetamodel()).thenReturn(mockModel);

        JpaDataStore store = new JpaDataStore(() -> { return managerMock; }, (unused) -> { return null; });
        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());


        try {
            store.populateEntityDictionary(dictionary);

            assertNotNull(FilterTranslator.lookupJPQLGenerator(Operator.IN, Test.class, "name"));
        } finally {
            FilterTranslator.registerJPQLGenerator(Operator.IN, Test.class, "name", null);
        }
    }

    @Test
    public void verifyManualEntityBinding() {

        @Include
        @Entity
        class Test {
            @Id
            private long id;

            private String name;
        }

        Metamodel mockModel = mock(Metamodel.class);
        when(mockModel.getEntities()).thenReturn(Sets.newHashSet());

        EntityManager managerMock = mock(EntityManager.class);
        when(managerMock.getMetamodel()).thenReturn(mockModel);

        JpaDataStore store = new JpaDataStore(() -> { return managerMock; }, (unused) -> { return null; }, Test.class);
        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());
        store.populateEntityDictionary(dictionary);

        assertNotNull(dictionary.lookupBoundClass(Test.class));
    }
}
