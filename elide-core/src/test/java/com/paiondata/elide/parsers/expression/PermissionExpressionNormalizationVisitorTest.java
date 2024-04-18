/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.parsers.expression;

import static com.paiondata.elide.core.dictionary.EntityDictionary.NO_VERSION;

import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.dictionary.EntityPermissions;
import com.paiondata.elide.core.dictionary.TestDictionary;
import com.paiondata.elide.core.request.route.Route;
import com.paiondata.elide.core.security.permissions.expressions.CheckExpression;
import com.paiondata.elide.core.security.permissions.expressions.Expression;
import com.paiondata.elide.core.security.visitors.PermissionExpressionNormalizationVisitor;
import com.paiondata.elide.core.security.visitors.PermissionExpressionVisitor;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.UUID;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PermissionExpressionNormalizationVisitorTest {

    private PermissionExpressionVisitor permissionExpressionVisitor;
    private PermissionExpressionNormalizationVisitor normalizationVisitor;

    @BeforeAll
    public void setUp() {
        EntityDictionary dictionary = TestDictionary.getTestDictionary();
        ElideSettings elideSettings = ElideSettings.builder().dataStore(null)
                .entityDictionary(dictionary)
                .build();
        Route route = Route.builder().apiVersion(NO_VERSION).build();
        RequestScope requestScope = RequestScope.builder().route(route).requestId(UUID.randomUUID())
                .elideSettings(elideSettings).build();

        permissionExpressionVisitor = new PermissionExpressionVisitor(dictionary,
                (check -> new CheckExpression(check, null, requestScope, null, null)));
        normalizationVisitor = new PermissionExpressionNormalizationVisitor();
    }

    @Test
    public void orExpressionTest() {
        ParseTree tree;
        Expression normalizedExpression;


        tree = EntityPermissions.parseExpression("not (sampleCommit or sampleOperation)");
        normalizedExpression = tree.accept(permissionExpressionVisitor).accept(normalizationVisitor);
        Assertions.assertEquals("(NOT ((sampleCommit \u001B[34mWAS UNEVALUATED\u001B[m))) AND "
                        + "(NOT ((sampleOperation \u001B[34mWAS UNEVALUATED\u001B[m)))",
                normalizedExpression.toString());

        tree = EntityPermissions.parseExpression("not (Prefab.Role.All or sampleCommit or initCheck)");
        normalizedExpression = tree.accept(permissionExpressionVisitor).accept(normalizationVisitor);
        Assertions.assertEquals("((NOT ((Prefab.Role.All \u001B[34mWAS UNEVALUATED\u001B[m))) AND "
                        + "(NOT ((sampleCommit \u001B[34mWAS UNEVALUATED\u001B[m)))) AND "
                        + "(NOT ((initCheck \u001B[34mWAS UNEVALUATED\u001B[m)))",
                normalizedExpression.toString());


        tree = EntityPermissions.parseExpression("not (parentInitCheck or passingOp) or (FailOp or shouldCache)");
        normalizedExpression = tree.accept(permissionExpressionVisitor).accept(normalizationVisitor);
        Assertions.assertEquals("((NOT ((parentInitCheck \u001B[34mWAS UNEVALUATED\u001B[m))) AND (NOT ((passingOp \u001B[34mWAS UNEVALUATED\u001B[m)))) OR "
                        + "(((FailOp \u001B[34mWAS UNEVALUATED\u001B[m)) OR ((shouldCache \u001B[34mWAS UNEVALUATED\u001B[m)))",
                normalizedExpression.toString());

        tree = EntityPermissions.parseExpression("not (parentInitCheck or passingOp) or not (FailOp or shouldCache)");
        normalizedExpression = tree.accept(permissionExpressionVisitor).accept(normalizationVisitor);
        Assertions.assertEquals("((NOT ((parentInitCheck \u001B[34mWAS UNEVALUATED\u001B[m))) AND (NOT ((passingOp \u001B[34mWAS UNEVALUATED\u001B[m)))) OR "
                        + "((NOT ((FailOp \u001B[34mWAS UNEVALUATED\u001B[m))) AND (NOT ((shouldCache \u001B[34mWAS UNEVALUATED\u001B[m))))",
                normalizedExpression.toString());

        tree = EntityPermissions.parseExpression("not (not (parentInitCheck or passingOp) or not (FailOp or shouldCache))");
        normalizedExpression = tree.accept(permissionExpressionVisitor).accept(normalizationVisitor);
        Assertions.assertEquals("(((parentInitCheck \u001B[34mWAS UNEVALUATED\u001B[m)) OR ((passingOp \u001B[34mWAS UNEVALUATED\u001B[m))) AND "
                        + "(((FailOp \u001B[34mWAS UNEVALUATED\u001B[m)) OR ((shouldCache \u001B[34mWAS UNEVALUATED\u001B[m)))",
                normalizedExpression.toString());
    }

    @Test
    public void andExpressionTest() {
        ParseTree tree;
        Expression normalizedExpression;


        tree = EntityPermissions.parseExpression("not (sampleCommit and sampleOperation)");
        normalizedExpression = tree.accept(permissionExpressionVisitor).accept(normalizationVisitor);
        Assertions.assertEquals("(NOT ((sampleCommit \u001B[34mWAS UNEVALUATED\u001B[m))) OR "
                        + "(NOT ((sampleOperation \u001B[34mWAS UNEVALUATED\u001B[m)))",
                normalizedExpression.toString());

        tree = EntityPermissions.parseExpression("not (Prefab.Role.All and sampleCommit and initCheck)");
        normalizedExpression = tree.accept(permissionExpressionVisitor).accept(normalizationVisitor);
        Assertions.assertEquals("((NOT ((Prefab.Role.All \u001B[34mWAS UNEVALUATED\u001B[m))) OR "
                        + "(NOT ((sampleCommit \u001B[34mWAS UNEVALUATED\u001B[m)))) OR "
                        + "(NOT ((initCheck \u001B[34mWAS UNEVALUATED\u001B[m)))",
                normalizedExpression.toString());


        tree = EntityPermissions.parseExpression("not (parentInitCheck and passingOp) and (FailOp and shouldCache)");
        normalizedExpression = tree.accept(permissionExpressionVisitor).accept(normalizationVisitor);
        Assertions.assertEquals("((NOT ((parentInitCheck \u001B[34mWAS UNEVALUATED\u001B[m))) OR (NOT ((passingOp \u001B[34mWAS UNEVALUATED\u001B[m)))) AND "
                        + "(((FailOp \u001B[34mWAS UNEVALUATED\u001B[m)) AND ((shouldCache \u001B[34mWAS UNEVALUATED\u001B[m)))",
                normalizedExpression.toString());

        tree = EntityPermissions.parseExpression("not (parentInitCheck and passingOp) and not (FailOp and shouldCache)");
        normalizedExpression = tree.accept(permissionExpressionVisitor).accept(normalizationVisitor);
        Assertions.assertEquals("((NOT ((parentInitCheck \u001B[34mWAS UNEVALUATED\u001B[m))) OR (NOT ((passingOp \u001B[34mWAS UNEVALUATED\u001B[m)))) AND "
                        + "((NOT ((FailOp \u001B[34mWAS UNEVALUATED\u001B[m))) OR (NOT ((shouldCache \u001B[34mWAS UNEVALUATED\u001B[m))))",
                normalizedExpression.toString());

        tree = EntityPermissions.parseExpression("not (not (parentInitCheck and passingOp) and not (FailOp and shouldCache))");
        normalizedExpression = tree.accept(permissionExpressionVisitor).accept(normalizationVisitor);
        Assertions.assertEquals("(((parentInitCheck \u001B[34mWAS UNEVALUATED\u001B[m)) AND ((passingOp \u001B[34mWAS UNEVALUATED\u001B[m))) OR "
                        + "(((FailOp \u001B[34mWAS UNEVALUATED\u001B[m)) AND ((shouldCache \u001B[34mWAS UNEVALUATED\u001B[m)))",
                normalizedExpression.toString());
    }

    @Test
    public void normalforms() {
        ParseTree tree;
        Expression normalizedExpression;


        tree = EntityPermissions.parseExpression("not ((parentInitCheck and passingOp) or shouldCache)");
        normalizedExpression = tree.accept(permissionExpressionVisitor).accept(normalizationVisitor);
        Assertions.assertEquals("((NOT ((parentInitCheck \u001B[34mWAS UNEVALUATED\u001B[m))) OR "
                        + "(NOT ((passingOp \u001B[34mWAS UNEVALUATED\u001B[m)))) AND "
                        + "(NOT ((shouldCache \u001B[34mWAS UNEVALUATED\u001B[m)))",
                normalizedExpression.toString());


        tree = EntityPermissions.parseExpression("not (parentInitCheck and (passingOp or shouldCache))");
        normalizedExpression = tree.accept(permissionExpressionVisitor).accept(normalizationVisitor);
        Assertions.assertEquals("(NOT ((parentInitCheck \u001B[34mWAS UNEVALUATED\u001B[m))) OR "
                        + "((NOT ((passingOp \u001B[34mWAS UNEVALUATED\u001B[m))) AND "
                        + "(NOT ((shouldCache \u001B[34mWAS UNEVALUATED\u001B[m))))",
                normalizedExpression.toString());

        tree = EntityPermissions.parseExpression("not parentInitCheck and not passingOp");
        normalizedExpression = tree.accept(permissionExpressionVisitor).accept(normalizationVisitor);
        Assertions.assertEquals("(NOT ((parentInitCheck \u001B[34mWAS UNEVALUATED\u001B[m))) AND "
                        + "(NOT ((passingOp \u001B[34mWAS UNEVALUATED\u001B[m)))",
                normalizedExpression.toString());

    }
}
