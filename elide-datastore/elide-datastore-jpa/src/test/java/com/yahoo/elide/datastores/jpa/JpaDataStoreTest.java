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
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.datastores.jpql.annotations.JPQLFilterFragment;
import com.yahoo.elide.datastores.jpql.filter.FilterTranslator;
import com.yahoo.elide.datastores.jpql.filter.JPQLPredicateGenerator;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;

import java.util.function.Function;

public class JpaDataStoreTest {
    public static class TestGenerator implements JPQLPredicateGenerator {
        @Override
        public String generate(FilterPredicate predicate, Function<Path, String> parameters) {
            return "FOO()";
        }
    }

    @Test
    public void verifyJPQLGeneratorRegistration() {

        @Include(rootLevel = false)
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

        JpaDataStore store = new JpaDataStore(() -> managerMock, unused -> null);
        EntityDictionary dictionary = EntityDictionary.builder().build();


        try {
            store.populateEntityDictionary(dictionary);

            assertNotNull(FilterTranslator.lookupJPQLGenerator(Operator.IN, ClassType.of(Test.class), "name"));
        } finally {
            FilterTranslator.registerJPQLGenerator(Operator.IN, ClassType.of(Test.class), "name", null);
        }
    }

    @Test
    public void verifyManualEntityBinding() {

        @Include(rootLevel = false)
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

        JpaDataStore store = new JpaDataStore(() -> managerMock, unused -> null, ClassType.of(Test.class));
        EntityDictionary dictionary = EntityDictionary.builder().build();
        store.populateEntityDictionary(dictionary);

        assertNotNull(dictionary.lookupBoundClass(ClassType.of(Test.class)));
    }
}
