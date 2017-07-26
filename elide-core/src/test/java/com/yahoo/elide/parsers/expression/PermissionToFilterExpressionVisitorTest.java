/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.parsers.expression;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.FilterExpressionCheck;
import com.yahoo.elide.security.RequestScope;
import com.yahoo.elide.security.User;
import com.yahoo.elide.security.checks.Check;
import com.yahoo.elide.security.checks.OperationCheck;
import com.yahoo.elide.security.checks.UserCheck;
import com.yahoo.elide.security.checks.prefab.Role;
import com.yahoo.elide.security.permissions.ExpressionResult;
import com.yahoo.elide.security.permissions.expressions.Expression;
import lombok.AllArgsConstructor;
import org.antlr.v4.runtime.tree.ParseTree;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.yahoo.elide.parsers.expression.PermissionToFilterExpressionVisitor.NO_EVALUATION_EXPRESSION;

public class PermissionToFilterExpressionVisitorTest {
    private EntityDictionary dictionary;
    private RequestScope requestScope;
    private ElideSettings elideSettings;
    private static FilterPredicate.PathElement AUTHORPATH = new FilterPredicate.PathElement(Author.class, "Author", Book.class, "books");
    private static FilterPredicate.PathElement BOOKPATH = new FilterPredicate.PathElement(Book.class, "Book", String.class, "title");
    private static List EXAPLEFIELDNAME = new ArrayList<>(Arrays.asList("Harry Potter"));

    public static final FilterExpressionNormalizationVisitor NORMALIZEVISITOR = new FilterExpressionNormalizationVisitor();

    @BeforeMethod
    public void setupEntityDictionary() {
        Map<String, Class<? extends Check>> checks = new HashMap<>();
        checks.put("Allow", Permissions.Succeeds.class);
        checks.put("Deny", Permissions.Fails.class);
        checks.put("user has all access", Role.ALL.class);
        checks.put("IN predicate", FilterExpressionCheck1.class);
        checks.put("NOTIN predicate", FilterExpressionCheck1Negated.class);
        checks.put("LESS than predicate", FilterExpressionCheck2.class);
        checks.put("GREATER than EQUAL to predicate", FilterExpressionCheck2Negated.class);
        checks.put("user has no access", Role.NONE.class);

        dictionary = new EntityDictionary(checks);
        dictionary.bindEntity(Model.class);
        dictionary.bindEntity(ComplexEntity.class);
        elideSettings = new ElideSettingsBuilder(null)
                .withEntityDictionary(dictionary)
                .build();

        requestScope = newRequestScope();

    }

    @Test
    public void testAndExpression() {
        Expression expression = getExpressionForPermission(ReadPermission.class);
        Assert.assertEquals(expression.evaluate(Expression.EvaluationMode.ALL_CHECKS), ExpressionResult.PASS);
    }

    @Test
    public void testOrExpression() {
        Expression expression = getExpressionForPermission(UpdatePermission.class);
        Assert.assertEquals(expression.evaluate(Expression.EvaluationMode.ALL_CHECKS), ExpressionResult.PASS);
    }

    @Test
    public void testNotExpression() {
        Expression expression = getExpressionForPermission(DeletePermission.class);
        Assert.assertEquals(expression.evaluate(Expression.EvaluationMode.ALL_CHECKS), ExpressionResult.PASS);
    }

    @Test
    public void testComplexExpression() {
        Expression expression = getExpressionForPermission(UpdatePermission.class);
        Assert.assertEquals(expression.evaluate(Expression.EvaluationMode.ALL_CHECKS), ExpressionResult.PASS);
    }

    @Test
    public void testComplexModelCreate() {
        Expression expression = getExpressionForPermission(CreatePermission.class, ComplexEntity.class);
        Assert.assertEquals(expression.evaluate(Expression.EvaluationMode.ALL_CHECKS), ExpressionResult.PASS);
    }

    @Test
    public void testNamesWithSpaces() {
        Expression expression = getExpressionForPermission(DeletePermission.class, ComplexEntity.class);
        Expression expression2 = getExpressionForPermission(UpdatePermission.class, ComplexEntity.class);
        Assert.assertEquals(expression.evaluate(Expression.EvaluationMode.ALL_CHECKS), ExpressionResult.PASS);
        Assert.assertEquals(expression2.evaluate(Expression.EvaluationMode.ALL_CHECKS), ExpressionResult.PASS);
    }

    @Test
    public void testAndOperationCheck() {
        @Entity
        @Include
        @ReadPermission(expression = "IN predicate AND Allow")
        class TargetClass {
        }
        FilterExpression expected = createDummyPredicate(Operator.IN);
        Assert.assertEquals(expected, extractFilterExpression(TargetClass.class));
    }

    @Test
    public void testOrWithUserCheck() {
        @Entity
        @Include
        @ReadPermission(expression = "IN predicate OR user has all access")
        class TargetClass {
        }

        Assert.assertTrue(extractFilterExpression(TargetClass.class) == PermissionToFilterExpressionVisitor.NO_EVALUATION_EXPRESSION);
    }

    @Test
    public void testNestedOrWithMixedUserCheck() {
        @Entity
        @Include
        @ReadPermission(expression = "IN predicate OR (user has all access AND user has all access)")
        class TargetClass {
        }

        Assert.assertTrue(extractFilterExpression(TargetClass.class) == PermissionToFilterExpressionVisitor.NO_EVALUATION_EXPRESSION);
    }

    @Test
    public void testNestedOrWithOmittedUserCheck() {
        @Entity
        @Include
        @ReadPermission(expression = "IN predicate OR (IN predicate AND user has all access)")
        class TargetClass {
        }

        FilterExpression dummy = createDummyPredicate(Operator.IN);
        FilterExpression expected = new OrFilterExpression(dummy, dummy);
        Assert.assertEquals(expected, extractFilterExpression(TargetClass.class));
    }

    @Test
    public void testNestedOrAndWithUserCheck() {
        @Entity
        @Include
        @ReadPermission(expression = "IN predicate OR (IN predicate OR user has all access)")
        class TargetClass {
        }

        Assert.assertTrue(extractFilterExpression(TargetClass.class) == PermissionToFilterExpressionVisitor.NO_EVALUATION_EXPRESSION);
    }

    @Test
    public void testNestedAndOrWithOmittedUserCheck() {
        @Entity
        @Include
        @ReadPermission(expression = "Allow AND (IN predicate OR IN predicate)")
        class TargetClass {
        }

        FilterExpression dummy = createDummyPredicate(Operator.IN);
        FilterExpression expected = new OrFilterExpression(dummy, dummy);
        Assert.assertEquals(expected, extractFilterExpression(TargetClass.class));
    }

    @Test
    public void testOperationCheck() {
        @Entity
        @Include
        @ReadPermission(expression = "Allow")
        class TargetClass {
        }

        Assert.assertTrue(extractFilterExpression(TargetClass.class) == PermissionToFilterExpressionVisitor.NO_EVALUATION_EXPRESSION);
    }

    @Test
    public void testNestedAndOrWithOnlyUserCheck() {
        @Entity
        @Include
        @ReadPermission(expression = "user has all access AND (user has all access OR user has all access)")
        class TargetClass {
        }

        Assert.assertTrue(extractFilterExpression(TargetClass.class) == PermissionToFilterExpressionVisitor.NO_EVALUATION_EXPRESSION);
    }

    @Test
    public void testNestedOrAndWithOperationUserCheck() {
        @Entity
        @Include
        @ReadPermission(expression = "user has all access OR (Allow AND user has all access)")
        class TargetClass {
        }

        Assert.assertTrue(extractFilterExpression(TargetClass.class) == PermissionToFilterExpressionVisitor.NO_EVALUATION_EXPRESSION);
    }

    @Test
    public void testNestedAndOrWithOperationUserCheck() {
        @Entity
        @Include
        @ReadPermission(expression = "user has all access AND (Allow OR user has all access)")
        class TargetClass {
        }

        Assert.assertTrue(extractFilterExpression(TargetClass.class) == PermissionToFilterExpressionVisitor.NO_EVALUATION_EXPRESSION);
    }

    @Test
    public void testNotpredicateWithIn() {
        //Test NOT with "IN" FilterPredicate
        @Entity
        @Include
        @ReadPermission(expression = "NOT (IN predicate)")
        class TargetClass {
        }
        @Entity
        @Include
        @ReadPermission(expression = "NOTIN predicate")
        class TargetClassNegated {
        }
        compareEqualNotFilterExpression(TargetClass.class, TargetClassNegated.class);
    }

    @Test
    public void testNotpredicateWithLT() {
        //Test NOT with "LT" FilterPredicate
        @Entity
        @Include
        @ReadPermission(expression = "NOT (LESS than predicate)")
        class TargetClass {
        }
        @Entity
        @Include
        @ReadPermission(expression = "GREATER than EQUAL to predicate")
        class TargetClassNegated {
        }
        compareEqualNotFilterExpression(TargetClass.class, TargetClassNegated.class);
    }

    @Test
    public void testNotWithAnd() {
        //Test Not with AND FilterExpression
        @Entity
        @Include
        @ReadPermission(expression = "NOT (IN predicate AND LESS than predicate)")
        class TargetClass {
        }
        @Entity
        @Include
        @ReadPermission(expression = "NOTIN predicate OR GREATER than EQUAL to predicate")
        class TargetClassNegated {
        }
        compareEqualNotFilterExpression(TargetClass.class, TargetClassNegated.class);
    }

    @Test
    public void testNotWithOr() {
        //Test Not with OR FilterExpression
        @Entity
        @Include
        @ReadPermission(expression = "NOT (IN predicate OR LESS than predicate)")
        class TargetClass {
        }
        @Entity
        @Include
        @ReadPermission(expression = "NOTIN predicate AND GREATER than EQUAL to predicate")
        class TargetClassNegated {
        }
        compareEqualNotFilterExpression(TargetClass.class, TargetClassNegated.class);
    }

    @Test
    public void testNotNested() {
        //Test Not with nested FilterExpression
        @Entity
        @Include
        @ReadPermission(expression = "NOT (IN predicate OR (LESS than predicate AND (NOTIN predicate)))")
        class TargetClass {
        }
        @Entity
        @Include
        @ReadPermission(expression = "NOTIN predicate AND (GREATER than EQUAL to predicate OR IN predicate)")
        class TargetClassNegated {
        }
        compareEqualNotFilterExpression(TargetClass.class, TargetClassNegated.class);
    }

    @Test
    public void testNotWithOperationCheck() {
        @Entity
        @Include
        @ReadPermission(expression = "NOT Allow")
        class TargetClass {
        }
        dictionary.bindEntity(TargetClass.class);
        ParseTree expression = dictionary.getPermissionsForClass(TargetClass.class, ReadPermission.class);
        PermissionToFilterExpressionVisitor fev;
        fev = new PermissionToFilterExpressionVisitor(dictionary, requestScope, TargetClass.class);
        FilterExpression fe = fev.visit(expression).accept(NORMALIZEVISITOR);
        Assert.assertTrue(fe == NO_EVALUATION_EXPRESSION);
    }

    private Expression getExpressionForPermission(Class<? extends Annotation> permission) {
        return getExpressionForPermission(permission, Model.class);
    }

    private Expression getExpressionForPermission(Class<? extends Annotation> permission, Class model) {
        PermissionExpressionVisitor v = new PermissionExpressionVisitor(dictionary, DummyExpression::new);
        ParseTree permissions = dictionary.getPermissionsForClass(model, permission);

        return v.visit(permissions);
    }

    public RequestScope newRequestScope() {
        User john = new User("John");
        return requestScope = new com.yahoo.elide.core.RequestScope(null, null, null, john, null, elideSettings);
    }

    @Entity
    @Include
    @ReadPermission(expression = "user has all access AND Allow")
    @UpdatePermission(expression = "Allow or Deny")
    @DeletePermission(expression = "Not Deny")
    @CreatePermission(expression = "not Allow or not (Deny and Allow)")
    static class Model {
    }

    public static class Permissions {
        public static class Succeeds extends OperationCheck<Model> {
            @Override
            public boolean ok(Model object, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
                return true;
            }
        }

        public static class Fails extends OperationCheck<Model> {
            @Override
            public boolean ok(Model object, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
                return false;
            }
        }
    }

    @Entity
    @Include
    @ReadPermission(expression = "IN predicate OR (user has all access OR Allow)")
    @CreatePermission(expression = "(Deny or Allow) and (not Deny)")
    @DeletePermission(expression = "user has all access or user has no access")
    @UpdatePermission(expression = "user has all access and (user has no access or user has all access)")
    static class ComplexEntity {
    }

    @Entity
    @Include(rootLevel = true)
    @ReadPermission(expression = "IN predicate AND user has all access")
    @CreatePermission(expression = "(Deny or Allow) and (not Deny)")
    @DeletePermission(expression = "user has all access or user has no access")
    @UpdatePermission(expression = "user has all access and (user has no access or user has all access)")
    private class Author {
        private long id;
        private String name;
        private Collection<Book> books = new ArrayList<>();

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @ManyToMany(mappedBy = "authors")
        public Collection<Book> getBooks() {
            return books;
        }

        public void setBooks(Collection<Book> books) {
            this.books = books;
        }
    }

    @Entity
    @Include(rootLevel = true)
    @ReadPermission(expression = "IN predicate AND user has all access")
    @CreatePermission(expression = "(Deny or Allow) and (not Deny)")
    @DeletePermission(expression = "user has all access or user has no access")
    @UpdatePermission(expression = "user has all access and (user has no access or user has all access)")
    private class Book {
        private long id;
        private String title;
        private String genre;
        private String language;
        private Collection<Author> authors = new ArrayList<>();

        @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getGenre() {
            return genre;
        }

        public void setGenre(String genre) {
            this.genre = genre;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        @ManyToMany
        public Collection<Author> getAuthors() {
            return authors;
        }

        public void setAuthors(Collection<Author> authors) {
            this.authors = authors;
        }
    }

    @AllArgsConstructor
    private class DummyExpression implements Expression {
        Check check;

        @Override
        public ExpressionResult evaluate(EvaluationMode ignored) {
            boolean result;
            if (check instanceof UserCheck) {
                result = ((UserCheck) check).ok(null);
            } else {
                result = check.ok(null, null, null);
            }

            if (result) {
                return ExpressionResult.PASS;
            } else {
                return ExpressionResult.FAIL;
            }
        }
    }

    private static FilterPredicate createDummyPredicate(Operator operator) {
        List<FilterPredicate.PathElement> pathList = new ArrayList<>(Arrays.asList(AUTHORPATH, BOOKPATH));
        Operator op = operator;
        List value = EXAPLEFIELDNAME;
        return new FilterPredicate(pathList, op, value);
    }

    private void compareEqualNotFilterExpression(Class target, Class targetNegated) {
        dictionary.bindEntity(target);
        dictionary.bindEntity(targetNegated);
        ParseTree expression1 = dictionary.getPermissionsForClass(target, ReadPermission.class);
        ParseTree expression2 = dictionary.getPermissionsForClass(targetNegated, ReadPermission.class);
        PermissionToFilterExpressionVisitor fev1, fev2;
        fev1 = new PermissionToFilterExpressionVisitor(dictionary, requestScope, target);
        fev2 = new PermissionToFilterExpressionVisitor(dictionary, requestScope, targetNegated);
        FilterExpression result1 = fev1.visit(expression1).accept(NORMALIZEVISITOR);
        FilterExpression result2 = fev2.visit(expression2).accept(NORMALIZEVISITOR);
        Assert.assertEquals(result1, result2);
    }

    private FilterExpression extractFilterExpression(Class targetClass) {
        dictionary.bindEntity(targetClass);
        ParseTree expression = dictionary.getPermissionsForClass(targetClass, ReadPermission.class);
        PermissionToFilterExpressionVisitor fev;
        fev = new PermissionToFilterExpressionVisitor(dictionary, requestScope, targetClass);
        FilterExpression fe = fev.visit(expression).accept(NORMALIZEVISITOR);
        return fe;
    }

    public static class FilterExpressionCheck1 extends FilterExpressionCheck {

        @Override
        public FilterPredicate getFilterExpression(Class entityClass, RequestScope requestScope) {
            return createDummyPredicate(Operator.IN);
        }
    }

    public static class FilterExpressionCheck2 extends FilterExpressionCheck {

        @Override
        public FilterPredicate getFilterExpression(Class entityClass, RequestScope requestScope) {
            return createDummyPredicate(Operator.LT);
        }
    }

    public static class FilterExpressionCheck1Negated extends FilterExpressionCheck {

        @Override
        public FilterPredicate getFilterExpression(Class entityClass, RequestScope requestScope) {
            return createDummyPredicate(Operator.NOT);
        }
    }

    public static class FilterExpressionCheck2Negated extends FilterExpressionCheck {

        @Override
        public FilterPredicate getFilterExpression(Class entityClass, RequestScope requestScope) {
            return createDummyPredicate(Operator.GE);
        }
    }
}
