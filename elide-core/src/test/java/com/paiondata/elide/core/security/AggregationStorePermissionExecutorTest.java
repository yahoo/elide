/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.core.security;

import static com.paiondata.elide.core.dictionary.EntityDictionary.NO_VERSION;

import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.annotation.Include;
import com.paiondata.elide.annotation.ReadPermission;
import com.paiondata.elide.core.Path;
import com.paiondata.elide.core.PersistentResource;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.dictionary.TestDictionary;
import com.paiondata.elide.core.exceptions.ForbiddenAccessException;
import com.paiondata.elide.core.filter.expression.FilterExpression;
import com.paiondata.elide.core.filter.predicates.NotNullPredicate;
import com.paiondata.elide.core.request.route.Route;
import com.paiondata.elide.core.security.checks.Check;
import com.paiondata.elide.core.security.checks.FilterExpressionCheck;
import com.paiondata.elide.core.security.checks.prefab.Role;
import com.paiondata.elide.core.security.executors.AggregationStorePermissionExecutor;
import com.paiondata.elide.core.security.permissions.ExpressionResult;
import com.paiondata.elide.core.type.ClassType;
import com.paiondata.elide.core.type.Type;
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
 * Test Aggregation Store Permission Executor.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AggregationStorePermissionExecutorTest {
    private EntityDictionary dictionary;
    private ElideSettings elideSettings;


    public static class FilterCheck extends FilterExpressionCheck<Object> {

        @Override
        public FilterExpression getFilterExpression(Type<?> entityClass, com.paiondata.elide.core.security.RequestScope requestScope) {
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
        elideSettings = ElideSettings.builder().entityDictionary(dictionary)
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

        com.paiondata.elide.core.RequestScope scope = bindAndgetRequestScope(Model.class);
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

        com.paiondata.elide.core.RequestScope scope = bindAndgetRequestScope(Model1.class);
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

    private com.paiondata.elide.core.RequestScope bindAndgetRequestScope(Class clz) {
        dictionary.bindEntity(clz);
        dictionary.bindPermissionExecutor(clz, AggregationStorePermissionExecutor::new);
        Route route = Route.builder().apiVersion(NO_VERSION).build();
        return com.paiondata.elide.core.RequestScope.builder().route(route).requestId(UUID.randomUUID())
                .elideSettings(elideSettings).build();
    }
}
