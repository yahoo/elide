/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.security;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.dictionary.TestDictionary;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.predicates.NotNullPredicate;
import com.yahoo.elide.core.security.checks.Check;
import com.yahoo.elide.core.security.checks.FilterExpressionCheck;
import com.yahoo.elide.core.security.checks.prefab.Role;
import com.yahoo.elide.core.security.executors.AggregationStorePermissionExecutor;
import com.yahoo.elide.core.security.permissions.ExpressionResult;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import jakarta.persistence.Entity;
import lombok.Value;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

/**
 * test Aggregation Store Permission Executor
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AggregationStorePermissionExecutorTest {
    private EntityDictionary dictionary;
    private ElideSettings elideSettings;


    public static class FilterCheck extends FilterExpressionCheck<Object> {

        @Override
        public FilterExpression getFilterExpression(Type<?> entityClass, com.yahoo.elide.core.security.RequestScope requestScope) {
            Path path = super.getFieldPath(entityClass, requestScope, "getFilterDim", "filterDim");
            return new NotNullPredicate(path);
        }
    }

    @BeforeAll
    public void setup() {
        Map<String, Class<? extends Check>> checks = new HashMap<>();
        checks.put("user all", Role.ALL.class);
        checks.put("user none", Role.NONE.class);
        checks.put("filter check", FilterCheck.class);
        dictionary = TestDictionary.getTestDictionary(checks);
        elideSettings = new ElideSettingsBuilder(null)
                .withEntityDictionary(dictionary)
                .build();
    }

    @Test
    public void testUserPermissions() {
        @Entity
        @Include
        @Value
        @ReadPermission(expression = "user none or filter check")
        class Model {
            String filterDim;
            long metric;
            long metric2;

            @ReadPermission(expression = "user all")
            public String getFilterDim() {
                return filterDim;
            }

            @ReadPermission(expression = "user none")
            public long getMetric() {
                return metric;
            }
        }

        com.yahoo.elide.core.RequestScope scope = bindAndgetRequestScope(Model.class);
        PermissionExecutor executor = scope.getPermissionExecutor();

        // evaluated expression = user all
        Assertions.assertEquals(
                ExpressionResult.PASS,
                executor.checkUserPermissions(ClassType.of(Model.class), ReadPermission.class, "filterDim"));


        // evaluated expression = user none -> ForbiddenAccess
        Assertions.assertThrows(
                ForbiddenAccessException.class,
                () -> executor.checkUserPermissions(ClassType.of(Model.class), ReadPermission.class, "metric"));


        // evaluated expression = null -> false
        Assertions.assertEquals(
                ExpressionResult.PASS,
                executor.checkSpecificFieldPermissions(
                        new PersistentResource(new Model("dim1", 0, 1), "1", scope),
                        null, ReadPermission.class, "metric2"));


        // evaluated expression = (user none or filter check) AND (user all OR user none)
        Assertions.assertEquals(
                ExpressionResult.DEFERRED,
                executor.checkUserPermissions(ClassType.of(Model.class), ReadPermission.class, new HashSet<>(Arrays.asList("filterDim", "metric"))));

        // evaluated expression = (user all OR user none)
        Assertions.assertEquals(
                ExpressionResult.PASS,
                executor.checkPermission(ReadPermission.class,
                        new PersistentResource(new Model("dim1", 0, 1), "1", scope),
                        new HashSet<>(Arrays.asList("filterDim", "metric"))));

        // evaluated expression = (user none OR null)
        Assertions.assertEquals(
                ExpressionResult.PASS,
                executor.checkPermission(ReadPermission.class,
                        new PersistentResource(new Model("dim1", 0, 1), "1", scope),
                        new HashSet<>(Arrays.asList("metric", "metric2"))));


    }

    @Test
    public void filterTest() {
        @Entity
        @Include
        @Value
        @ReadPermission(expression = "user none or filter check")
        class Model1 {
            String filterDim;
            long metric;
        }

        com.yahoo.elide.core.RequestScope scope = bindAndgetRequestScope(Model1.class);
        PermissionExecutor executor = scope.getPermissionExecutor();

        FilterExpression expression = executor.getReadPermissionFilter(ClassType.of(Model1.class),
                new HashSet<>(Arrays.asList("filterDim", "metric")))
                .orElse(null);
        Assertions.assertNotNull(expression);
        Assertions.assertEquals("model1.filterDim NOTNULL []", expression.toString());


        @Entity
        @Include
        @Value
        @ReadPermission(expression = "user none and filter check")
        class Model2 {
            String filterDim;
            long metric;
        }

        scope = bindAndgetRequestScope(Model2.class);
        executor = scope.getPermissionExecutor();

        expression = executor.getReadPermissionFilter(ClassType.of(Model2.class), new HashSet<>(Arrays.asList("filterDim", "metric"))).orElse(null);
        Assertions.assertNull(expression);

        @Entity
        @Include
        @Value
        @ReadPermission(expression = "user all or filter check")
        class Model3 {
            String filterDim;
            long metric;
        }

        scope = bindAndgetRequestScope(Model3.class);
        executor = scope.getPermissionExecutor();

        expression = executor.getReadPermissionFilter(ClassType.of(Model3.class), new HashSet<>(Arrays.asList("filterDim", "metric"))).orElse(null);
        Assertions.assertNull(expression);
    }

    private com.yahoo.elide.core.RequestScope bindAndgetRequestScope(Class clz) {
        dictionary.bindEntity(clz);
        dictionary.bindPermissionExecutor(clz, AggregationStorePermissionExecutor::new);
        return new com.yahoo.elide.core.RequestScope(null, null, NO_VERSION, null, null, null, null, null, UUID.randomUUID(), elideSettings);
    }
}
