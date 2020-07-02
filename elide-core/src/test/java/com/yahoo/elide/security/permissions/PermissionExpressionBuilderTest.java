/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.permissions;

import static com.yahoo.elide.core.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.TestDictionary;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.checks.Check;
import com.yahoo.elide.security.checks.prefab.Role;
import com.yahoo.elide.security.permissions.expressions.Expression;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.Id;

public class PermissionExpressionBuilderTest {

    private EntityDictionary dictionary;
    private PermissionExpressionBuilder builder;
    private ElideSettings elideSettings;

    @BeforeEach
    public void setupEntityDictionary() {
        Map<String, Class<? extends Check>> checks = new HashMap<>();
        checks.put("user has all access", Role.ALL.class);
        checks.put("user has no access", Role.NONE.class);

        dictionary = TestDictionary.getTestDictionary(checks);

        ExpressionResultCache cache = new ExpressionResultCache();
        builder = new PermissionExpressionBuilder(cache, dictionary);

        elideSettings = new ElideSettingsBuilder(null)
                .withEntityDictionary(dictionary)
                .build();
    }

    @Test
    public void testAnyFieldExpressionText() {
        @Entity
        @Include
        @ReadPermission(expression = "user has all access AND user has no access")
        class Model { }
        dictionary.bindEntity(Model.class);

        PersistentResource resource = newResource(new Model(), Model.class);

        Expression expression = builder.buildAnyFieldExpressions(
                resource,
                ReadPermission.class,
                null);

        assertEquals(
                "READ PERMISSION WAS INVOKED ON PersistentResource{type=model, id=null}  "
                        + "FOR EXPRESSION [(FIELDS(\u001B[31mFAILURE\u001B[m)) OR (ENTITY(((user has all access "
                        + "\u001B[34mWAS UNEVALUATED\u001B[m)) AND ((user has no access "
                        + "\u001B[34mWAS UNEVALUATED\u001B[m))))]",
                expression.toString());

        expression.evaluate(Expression.EvaluationMode.ALL_CHECKS);

        assertEquals(
                "READ PERMISSION WAS INVOKED ON PersistentResource{type=model, id=null}  "
                        + "FOR EXPRESSION [(FIELDS(\u001B[31mFAILURE\u001B[m)) OR (ENTITY(((user has all access "
                        + "\u001B[32mPASSED\u001B[m)) AND ((user has no access "
                        + "\u001B[31mFAILED\u001B[m))))]",
                expression.toString());

    }

    @Test
    public void testSpecificFieldExpressionText() {
        @Entity
        @Include
        @UpdatePermission(expression = "user has no access")
        class Model {
            @Id
            private long id;
            @UpdatePermission(expression = "user has all access OR user has no access")
            private int foo;
        }

        dictionary.bindEntity(Model.class);

        PersistentResource resource = newResource(new Model(), Model.class);
        ChangeSpec changes = new ChangeSpec(resource, "foo", 1, 2);

        Expression expression = builder.buildSpecificFieldExpressions(
                resource,
                UpdatePermission.class,
                "foo",
                changes);

        assertEquals(
                "UPDATE PERMISSION WAS INVOKED ON PersistentResource{type=model, id=0} WITH CHANGES ChangeSpec { "
                        + "resource=PersistentResource{type=model, id=0}, field=foo, original=1, modified=2} "
                        + "FOR EXPRESSION [FIELD(((user has all access "
                        + "\u001B[34mWAS UNEVALUATED\u001B[m)) OR ((user has no access "
                        + "\u001B[34mWAS UNEVALUATED\u001B[m)))]",
                expression.toString());

        expression.evaluate(Expression.EvaluationMode.ALL_CHECKS);

        assertEquals(
                "UPDATE PERMISSION WAS INVOKED ON PersistentResource{type=model, id=0} WITH CHANGES ChangeSpec { "
                        + "resource=PersistentResource{type=model, id=0}, field=foo, original=1, modified=2} "
                        + "FOR EXPRESSION [FIELD(((user has all access "
                        + "\u001B[32mPASSED\u001B[m)) OR ((user has no access "
                        + "\u001B[34mWAS UNEVALUATED\u001B[m)))]",
                expression.toString());

     }

    public <T> PersistentResource newResource(T obj, Class<T> cls) {
        RequestScope requestScope = new RequestScope(null, NO_VERSION, null, null, null, null, UUID.randomUUID(), elideSettings);
        return new PersistentResource<>(obj, null, requestScope.getUUIDFor(obj), requestScope);
    }
}
