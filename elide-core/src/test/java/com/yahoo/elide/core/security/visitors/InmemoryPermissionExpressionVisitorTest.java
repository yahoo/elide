package com.yahoo.elide.core.security.visitors;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.dictionary.EntityPermissions;
import com.yahoo.elide.core.dictionary.TestDictionary;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.predicates.NotNullPredicate;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.checks.Check;
import com.yahoo.elide.core.security.checks.DatastoreEvalFilterExpressionCheck;
import com.yahoo.elide.core.security.checks.FilterExpressionCheck;
import com.yahoo.elide.core.security.checks.OperationCheck;
import com.yahoo.elide.core.security.checks.prefab.Role;
import com.yahoo.elide.core.security.permissions.ExpressionResult;
import com.yahoo.elide.core.security.permissions.ExpressionResultCache;
import com.yahoo.elide.core.security.permissions.expressions.AndExpression;
import com.yahoo.elide.core.security.permissions.expressions.CheckExpression;
import com.yahoo.elide.core.security.permissions.expressions.Expression;
import com.yahoo.elide.core.security.permissions.expressions.ExpressionVisitor;
import com.yahoo.elide.core.security.permissions.expressions.NotExpression;
import com.yahoo.elide.core.security.permissions.expressions.OrExpression;
import com.yahoo.elide.core.type.Type;

import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import lombok.Value;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.persistence.Entity;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class InmemoryPermissionExpressionVisitorTest {
    private EntityDictionary dictionary;
    private ElideSettings elideSettings;
    private PermissionExpressionNormalizationVisitor normalizationVisitor;
    private Stringify stringify;
    private RequestScope requestScope;

    @Entity
    @Include
    @Value
    public class Model {
        int id;
        String operationDim;
        String filterDim;
    }

    public static class FilterCheck extends FilterExpressionCheck<Model> {

        @Override
        public FilterExpression getFilterExpression(Type<?> entityClass, com.yahoo.elide.core.security.RequestScope requestScope) {
            Path path = super.getFieldPath(entityClass, requestScope, "getFilterDim", "filterDim");
            return new NotNullPredicate(path);
        }
    }

    public static class Operationcheck extends OperationCheck<Model> {

        @Override
        public boolean ok(Model model, com.yahoo.elide.core.security.RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            return model.operationDim != null;
        }
    }

    @BeforeAll
    public void setupEntityDictionary() {
        Map<String, Class<? extends Check>> checks = new HashMap<>();
        checks.put("user all", Role.ALL.class);
        checks.put("user none", Role.NONE.class);
        checks.put("filter check", FilterCheck.class);
        checks.put("operation check", Operationcheck.class);
        dictionary = TestDictionary.getTestDictionary(checks);
        dictionary.bindEntity(Model.class);
        elideSettings = new ElideSettingsBuilder(null)
                .withEntityDictionary(dictionary)
                .build();
        requestScope = new RequestScope(null, null, NO_VERSION, null, null, null, null, null, UUID.randomUUID(), elideSettings);
        stringify = new Stringify();
        normalizationVisitor = new PermissionExpressionNormalizationVisitor();


    }

    @Test
    public void simpleFilterCheckAndUserCheck() {

        String permissionString;
        String expectedExpressionToString;

        ParseTree permissions;
        Pair<Expression, Boolean> expressionPair;
        Model model = new Model(1, null, null);

        InmemoryPermissionExpressionVisitor inmemoryPermissionVisitor = new InmemoryPermissionExpressionVisitor(dictionary, requestScope,
                (check) -> new CheckExpression(check, new PersistentResource<>(model, requestScope.getUUIDFor(model), requestScope), requestScope, null, new ExpressionResultCache()));


        //Only filter check is present. Should have run in memory
        permissionString = "filter check";
        expectedExpressionToString = "DatastoreEvalFilterExpressionCheck{" +
                        "executedInMemory=false, " +
                        "negated=false, " +
                        "wrappedFilterExpressionCheck=class com.yahoo.elide.core.security.visitors.InmemoryPermissionExpressionVisitorTest$FilterCheck}";
        exececuteAndCheckPermission(permissionString, inmemoryPermissionVisitor, expectedExpressionToString, ExpressionResult.PASS, false);

        permissionString = "user all or filter check";
        expectedExpressionToString = "\u001B[32mSUCCESS\u001B[m";
        exececuteAndCheckPermission(permissionString, inmemoryPermissionVisitor, expectedExpressionToString, ExpressionResult.PASS, false);

        permissionString = "user none or filter check";
        expectedExpressionToString = "DatastoreEvalFilterExpressionCheck{" +
                        "executedInMemory=false, " +
                        "negated=false, " +
                        "wrappedFilterExpressionCheck=class com.yahoo.elide.core.security.visitors.InmemoryPermissionExpressionVisitorTest$FilterCheck}";
        exececuteAndCheckPermission(permissionString, inmemoryPermissionVisitor, expectedExpressionToString, ExpressionResult.PASS, false);

        permissionString = "user all and filter check";
        expectedExpressionToString = "DatastoreEvalFilterExpressionCheck{" +
                        "executedInMemory=false, " +
                        "negated=false, " +
                        "wrappedFilterExpressionCheck=class com.yahoo.elide.core.security.visitors.InmemoryPermissionExpressionVisitorTest$FilterCheck}";
        exececuteAndCheckPermission(permissionString, inmemoryPermissionVisitor, expectedExpressionToString, ExpressionResult.PASS, false);

        permissionString = "filter check and user none";
        expectedExpressionToString = "\u001B[31mFAILURE\u001B[m";
        exececuteAndCheckPermission(permissionString, inmemoryPermissionVisitor, expectedExpressionToString, ExpressionResult.FAIL, false);

    }

    @Test
    public void inmemoryExecutorTest() {

        String permissionString;
        String expectedExpressionToString;

        //When operation check => true and filter check => false
        Model model = new Model(1, "exist", null);

        InmemoryPermissionExpressionVisitor inmemoryPermissionVisitor = new InmemoryPermissionExpressionVisitor(dictionary, requestScope,
                (check) -> new CheckExpression(check, new PersistentResource<>(model, requestScope.getUUIDFor(model), requestScope), requestScope, null, new ExpressionResultCache()));


        // Filter check is assumed to have evaluated in datastore (though filter dim will evaluate to false in memory - caused by group by operation).
        // Inmemory filter should evaluate only operation check hence overall result = true
        permissionString = "operation check and filter check";
        expectedExpressionToString = "((operation check \u001B[34mWAS UNEVALUATED\u001B[m))" +
                " AND " +
                "(DatastoreEvalFilterExpressionCheck{" +
                "executedInMemory=false, " +
                "negated=false, " +
                "wrappedFilterExpressionCheck=class com.yahoo.elide.core.security.visitors.InmemoryPermissionExpressionVisitorTest$FilterCheck})";
        exececuteAndCheckPermission(permissionString, inmemoryPermissionVisitor, expectedExpressionToString, ExpressionResult.PASS, true);


        // Not expression present on filter check should not affect the result as datastore handles it.
        // Therefore after normalization we should see any change in toString method except negated value in DatastoreEvalFilterExpressionCheck
        permissionString = "operation check and not filter check";
        expectedExpressionToString = "((operation check \u001B[34mWAS UNEVALUATED\u001B[m))" +
                " AND " +
                "(DatastoreEvalFilterExpressionCheck{" +
                "executedInMemory=false, " +
                "negated=true, " +
                "wrappedFilterExpressionCheck=class com.yahoo.elide.core.security.visitors.InmemoryPermissionExpressionVisitorTest$FilterCheck})";
        exececuteAndCheckPermission(permissionString, inmemoryPermissionVisitor, expectedExpressionToString, ExpressionResult.PASS, true);

        // Evaluates to (NOT (operationcheck = true)) AND (NOT (filter check))
        // NOT filter check will be evaluated in datastore and will be true for all records returned from datastore..
        // => (NOT (operationcheck = true)) AND (true)
        // => false
        permissionString = "not operation check and not filter check";
        expectedExpressionToString = "(NOT ((operation check \u001B[34mWAS UNEVALUATED\u001B[m)))" +
                " AND " +
                "(DatastoreEvalFilterExpressionCheck{" +
                "executedInMemory=false, " +
                "negated=true, " +
                "wrappedFilterExpressionCheck=class com.yahoo.elide.core.security.visitors.InmemoryPermissionExpressionVisitorTest$FilterCheck})";
        exececuteAndCheckPermission(permissionString, inmemoryPermissionVisitor, expectedExpressionToString, ExpressionResult.FAIL, true);


        permissionString = "not (operation check or filter check)";
        expectedExpressionToString = "(NOT ((operation check \u001B[34mWAS UNEVALUATED\u001B[m))) " +
                "AND " +
                "(DatastoreEvalFilterExpressionCheck{" +
                "executedInMemory=true, " +
                "negated=true, " +
                "wrappedFilterExpressionCheck=class com.yahoo.elide.core.security.visitors.InmemoryPermissionExpressionVisitorTest$FilterCheck})";
        exececuteAndCheckPermission(permissionString, inmemoryPermissionVisitor, expectedExpressionToString, ExpressionResult.FAIL, true);


        // Or expression does not push filter check to memory. Hence we evaluate filter check in memory.
        // therefore the overall expression evalautes to -
        // => (operation check = true) OR (filter check = false in memory).
        // => true
        permissionString = "operation check or filter check";
        expectedExpressionToString = "((operation check \u001B[34mWAS UNEVALUATED\u001B[m))" +
                " OR " +
                "(DatastoreEvalFilterExpressionCheck{" +
                "executedInMemory=true, " +
                "negated=false, " +
                "wrappedFilterExpressionCheck=class com.yahoo.elide.core.security.visitors.InmemoryPermissionExpressionVisitorTest$FilterCheck})";
        exececuteAndCheckPermission(permissionString, inmemoryPermissionVisitor, expectedExpressionToString, ExpressionResult.PASS, true);

        // Not expression for operation check changes the result because now we evaluate -
        // (Not (operation check = true)) OR (filter check = false in memory).
        // => (NOT (true)) OR false
        // => false
        permissionString = "not operation check or filter check";
        expectedExpressionToString = "(NOT ((operation check \u001B[34mWAS UNEVALUATED\u001B[m)))" +
                " OR " +
                "(DatastoreEvalFilterExpressionCheck{" +
                "executedInMemory=true, " +
                "negated=false, " +
                "wrappedFilterExpressionCheck=class com.yahoo.elide.core.security.visitors.InmemoryPermissionExpressionVisitorTest$FilterCheck})";
        exececuteAndCheckPermission(permissionString, inmemoryPermissionVisitor, expectedExpressionToString, ExpressionResult.FAIL, true);


        // Not on both operation and filter combined with OR.
        // (Not (operation check = true)) OR (NOT (filter check = false in memory)).
        // => (NOT (true)) OR (NOT (false))
        // => true
        permissionString = "not operation check or not filter check";
        expectedExpressionToString = "(NOT ((operation check \u001B[34mWAS UNEVALUATED\u001B[m)))" +
                " OR " +
                "(DatastoreEvalFilterExpressionCheck{" +
                "executedInMemory=true, " +
                "negated=true, " +
                "wrappedFilterExpressionCheck=class com.yahoo.elide.core.security.visitors.InmemoryPermissionExpressionVisitorTest$FilterCheck})";
        exececuteAndCheckPermission(permissionString, inmemoryPermissionVisitor, expectedExpressionToString, ExpressionResult.PASS, true);


        permissionString = "not (operation check and filter check)";
        expectedExpressionToString = "(NOT ((operation check \u001B[34mWAS UNEVALUATED\u001B[m)))" +
                " OR " +
                "(DatastoreEvalFilterExpressionCheck{" +
                "executedInMemory=false, " +
                "negated=true, " +
                "wrappedFilterExpressionCheck=class com.yahoo.elide.core.security.visitors.InmemoryPermissionExpressionVisitorTest$FilterCheck})";
        exececuteAndCheckPermission(permissionString, inmemoryPermissionVisitor, expectedExpressionToString, ExpressionResult.PASS, true);
    }


    @Test
    public void compositePermissionTest() {

        String permissionString;
        String expectedExpressionToString;
        //When operation check => false and filter check => true
        Model model = new Model(1, null, "exist");

        InmemoryPermissionExpressionVisitor inmemoryPermissionVisitor = new InmemoryPermissionExpressionVisitor(dictionary, requestScope,
                (check) -> new CheckExpression(check, new PersistentResource<>(model, requestScope.getUUIDFor(model), requestScope), requestScope, null, new ExpressionResultCache()));


        permissionString = "user all or (operation check and filter check)";
        expectedExpressionToString = "\u001B[32mSUCCESS\u001B[m";
        exececuteAndCheckPermission(permissionString, inmemoryPermissionVisitor, expectedExpressionToString, ExpressionResult.PASS, false);


        permissionString = "operation check or (user none and filter check)";
        expectedExpressionToString = "(operation check \u001B[34mWAS UNEVALUATED\u001B[m)";
        exececuteAndCheckPermission(permissionString, inmemoryPermissionVisitor, expectedExpressionToString, ExpressionResult.FAIL, true);


        // Presence of OR condition changes the filter check to be executed in memory
        permissionString = "operation check or (operation check and filter check)";
        expectedExpressionToString = "((operation check \u001B[34mWAS UNEVALUATED\u001B[m)) " +
                "OR " +
                "(((operation check \u001B[34mWAS UNEVALUATED\u001B[m)) " +
                "AND " +
                "(DatastoreEvalFilterExpressionCheck{" +
                "executedInMemory=true, " +
                "negated=false, " +
                "wrappedFilterExpressionCheck=class com.yahoo.elide.core.security.visitors.InmemoryPermissionExpressionVisitorTest$FilterCheck}))";
        exececuteAndCheckPermission(permissionString, inmemoryPermissionVisitor, expectedExpressionToString, ExpressionResult.FAIL, true);


    }

    private void exececuteAndCheckPermission(String permissionStr,
                                InmemoryPermissionExpressionVisitor inmemoryPermissionVisitor,
                                String expectedExpressionToString,
                                ExpressionResult expectedResult,
                                boolean containsInmemoryComponent) {

        ParseTree permissions = EntityPermissions.parseExpression(permissionStr);
        Pair<Expression, Boolean> expressionPair = inmemoryPermissionVisitor.visit(permissions);
        Expression normalizedExpression = expressionPair.getKey().accept(normalizationVisitor);
        Assertions.assertEquals(expectedExpressionToString, normalizedExpression.accept(stringify));
        Assertions.assertEquals(expectedResult, normalizedExpression.evaluate(Expression.EvaluationMode.ALL_CHECKS));
        Assertions.assertEquals(containsInmemoryComponent, expressionPair.getValue());

    }

    private class Stringify implements ExpressionVisitor<String> {

        @Override
        public String visitExpression(Expression expression) {
            return expression.toString();
        }

        @Override
        public String visitCheckExpression(CheckExpression checkExpression) {
            if (checkExpression.getCheck() instanceof DatastoreEvalFilterExpressionCheck) {
                return checkExpression.getCheck().toString();
            }
            return checkExpression.toString();
        }

        @Override
        public String visitAndExpression(AndExpression andExpression) {
            return String.format("(%s) AND (%s)",
                    andExpression.getLeft().accept(this),
                    andExpression.getRight().accept(this));
        }

        @Override
        public String visitOrExpression(OrExpression orExpression) {
            return String.format("(%s) OR (%s)",
                    orExpression.getLeft().accept(this),
                    orExpression.getRight().accept(this));
        }

        @Override
        public String visitNotExpression(NotExpression notExpression) {
            return String.format("NOT (%s)", notExpression.getLogical().accept(this));
        }
    }

}
