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
import com.yahoo.elide.core.annotations.JPQLFilterFragment;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.FilterTranslator;
import com.yahoo.elide.core.filter.JPQLPredicateGenerator;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.type.ClassType;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.function.Function;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;

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
        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());


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
        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());
        store.populateEntityDictionary(dictionary);

        assertNotNull(dictionary.lookupBoundClass(ClassType.of(Test.class)));
    }
}
