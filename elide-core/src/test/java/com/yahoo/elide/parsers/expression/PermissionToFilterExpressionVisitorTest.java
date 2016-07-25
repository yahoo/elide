/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.parsers.expression;

import static com.yahoo.elide.parsers.expression.PermissionToFilterExpressionVisitor.NO_EVALUATION_EXPRESSION;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.Predicate;
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

import org.antlr.v4.runtime.tree.ParseTree;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import lombok.AllArgsConstructor;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

public class PermissionToFilterExpressionVisitorTest {
    private EntityDictionary dictionary;
    private RequestScope requestScope;

    @BeforeMethod
    public void setupEntityDictionary() {
        Map<String, Class<? extends Check>> checks = new HashMap<>();
        checks.put("Allow", Permissions.Succeeds.class);
        checks.put("Deny", Permissions.Fails.class);
        checks.put("user has all access", Role.ALL.class);
        checks.put("owner is user", FilterExpressionCheck1.class);
        checks.put("user has no access", Role.NONE.class);

        dictionary = new EntityDictionary(checks);
        dictionary.bindEntity(Model.class);
        dictionary.bindEntity(ComplexEntity.class);
        dictionary.bindEntity(Good1.class);
        dictionary.bindEntity(Good2.class);
        dictionary.bindEntity(Good3.class);
        dictionary.bindEntity(Good4.class);
        dictionary.bindEntity(Good5.class);
        dictionary.bindEntity(Good6.class);
        dictionary.bindEntity(Good7.class);
        dictionary.bindEntity(GOOD8.class);
        dictionary.bindEntity(BAD1.class);
        dictionary.bindEntity(BAD2.class);
        dictionary.bindEntity(BAD4.class);
        dictionary.bindEntity(GOOD9.class);
        dictionary.bindEntity(GOOD10.class);
        requestScope = newRequestScope();
    }

    @Test
    public void testParseCombinationExpression() {
        ParseTree g1 = dictionary.getPermissionsForClass(Good1.class, ReadPermission.class);
        ParseTree g2 = dictionary.getPermissionsForClass(Good2.class, ReadPermission.class);
        ParseTree g3 = dictionary.getPermissionsForClass(Good3.class, ReadPermission.class);
        ParseTree g4 = dictionary.getPermissionsForClass(Good4.class, ReadPermission.class);
        ParseTree g5 = dictionary.getPermissionsForClass(Good5.class, ReadPermission.class);
        ParseTree g6 = dictionary.getPermissionsForClass(Good6.class, ReadPermission.class);
        ParseTree g7 = dictionary.getPermissionsForClass(Good7.class, ReadPermission.class);
        ParseTree g8 = dictionary.getPermissionsForClass(GOOD8.class, ReadPermission.class);
        ParseTree b1 = dictionary.getPermissionsForClass(BAD1.class, ReadPermission.class);
        ParseTree b2 = dictionary.getPermissionsForClass(BAD2.class, ReadPermission.class);
        ParseTree b4 = dictionary.getPermissionsForClass(BAD4.class, ReadPermission.class);
        ParseTree g9 = dictionary.getPermissionsForClass(GOOD9.class, ReadPermission.class);
        ParseTree g10 = dictionary.getPermissionsForClass(GOOD10.class, ReadPermission.class);
        PermissionToFilterExpressionVisitor fev = new PermissionToFilterExpressionVisitor(dictionary, requestScope);
        FilterExpression expected = createDummyPredicate();
        FilterExpression feg1 = fev.visit(g1);
        Assert.assertEquals(expected, feg1);
        FilterExpression feg2 = fev.visit(g2);
        Assert.assertTrue(feg2 == PermissionToFilterExpressionVisitor.NO_EVALUATION_EXPRESSION);
        //Assert.assertEquals(expected, feg2);
        FilterExpression feg3 = fev.visit(g3);
        Assert.assertTrue(feg3  == PermissionToFilterExpressionVisitor.NO_EVALUATION_EXPRESSION);
        FilterExpression feg4 = fev.visit(g4);
        Assert.assertEquals(new OrFilterExpression(expected, expected), feg4);
        FilterExpression feg5 = fev.visit(g5);
        Assert.assertTrue(feg5  == PermissionToFilterExpressionVisitor.NO_EVALUATION_EXPRESSION);
        FilterExpression feg6 = fev.visit(g6);
        Assert.assertEquals(new OrFilterExpression(expected, expected), feg6);
        FilterExpression feg7 = fev.visit(g7);
        Assert.assertTrue(feg7 == NO_EVALUATION_EXPRESSION);
        FilterExpression feg8 = fev.visit(g8);
        Assert.assertTrue(feg8  == PermissionToFilterExpressionVisitor.NO_EVALUATION_EXPRESSION);
        FilterExpression feb2 = fev.visit(b2);
        Assert.assertTrue(feb2 == NO_EVALUATION_EXPRESSION || feb2  == PermissionToFilterExpressionVisitor.FALSE_USER_CHECK_EXPRESSION);
        FilterExpression feb4 = fev.visit(b4);
        Assert.assertTrue(feb4 == NO_EVALUATION_EXPRESSION || feb4  == PermissionToFilterExpressionVisitor.FALSE_USER_CHECK_EXPRESSION);
        FilterExpression feg9 = fev.visit(g9);
        Assert.assertTrue(feg9 == NO_EVALUATION_EXPRESSION || feg9  == PermissionToFilterExpressionVisitor.FALSE_USER_CHECK_EXPRESSION);
        FilterExpression feg10 = fev.visit(g10);
        Assert.assertTrue(feg10 == NO_EVALUATION_EXPRESSION || feg10  == PermissionToFilterExpressionVisitor.FALSE_USER_CHECK_EXPRESSION);
    }

    @Test
    public void testAndExpression() {
        Expression expression = getExpressionForPermission(ReadPermission.class);
        Assert.assertEquals(expression.evaluate(), ExpressionResult.PASS);
    }

    @Test
    public void testOrExpression() {
        Expression expression = getExpressionForPermission(UpdatePermission.class);
        Assert.assertEquals(expression.evaluate(), ExpressionResult.PASS);
    }

    @Test
    public void testNotExpression() {
        Expression expression = getExpressionForPermission(DeletePermission.class);
        Assert.assertEquals(expression.evaluate(), ExpressionResult.PASS);
    }

    @Test
    public void testComplexExpression() {
        Expression expression = getExpressionForPermission(UpdatePermission.class);
        Assert.assertEquals(expression.evaluate(), ExpressionResult.PASS);
    }

    @Test
    public void testComplexModelCreate() {
        Expression expression = getExpressionForPermission(CreatePermission.class, ComplexEntity.class);
        Assert.assertEquals(expression.evaluate(), ExpressionResult.PASS);
    }

    @Test
    public void testNamesWithSpaces() {
        Expression expression = getExpressionForPermission(DeletePermission.class, ComplexEntity.class);
        Expression expression2 = getExpressionForPermission(UpdatePermission.class, ComplexEntity.class);
        Assert.assertEquals(expression.evaluate(), ExpressionResult.PASS);
        Assert.assertEquals(expression2.evaluate(), ExpressionResult.PASS);
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
        return requestScope = new com.yahoo.elide.core.RequestScope(null, null, null, john, dictionary, null, null);
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
    @ReadPermission(expression = "owner is user OR (user has all access OR Allow)")
    @CreatePermission(expression = "(Deny or Allow) and (not Deny)")
    @DeletePermission(expression = "user has all access or user has no access")
    @UpdatePermission(expression = "user has all access and (user has no access or user has all access)")
    static class ComplexEntity {
    }

    @Entity
    @Include
    @ReadPermission(expression = "owner is user AND Allow")
    static class Good1 {
    }

    @Entity
    @Include
    @ReadPermission(expression = "owner is user OR user has all access")
    static class Good2 {
    }

    @Entity
    @Include
    @ReadPermission(expression = "owner is user OR (user has all access AND user has all access)")
    static class Good3 {
    }

    @Entity
    @Include
    @ReadPermission(expression = "owner is user OR (owner is user AND user has all access)")
    static class Good4 {
    }

    @Entity
    @Include
    @ReadPermission(expression = "owner is user OR (owner is user OR user has all access)")
    static class Good5 {
    }

    @Entity
    @Include
    @ReadPermission(expression = "Allow AND (owner is user OR owner is user)")
    static class Good6 {
    }

    @Entity
    @Include
    @ReadPermission(expression = "Allow")
    static class Good7 {
    }

    @Entity
    @Include
    @ReadPermission(expression = "owner is user OR (Allow OR user has all access)")
    static class BAD1 {
    }

    @Entity
    @Include
    @ReadPermission(expression = "owner is user OR (Allow AND user has all access)")
    static class BAD2 {
    }

    @Entity
    @Include
    @ReadPermission(expression = "owner is user OR (Allow OR owner is user)")
    static class BAD4 {
    }

    @Entity
    @Include
    @ReadPermission(expression = "user has all access OR (Allow AND user has all access)")
    static class GOOD9 {
    }

    @Entity
    @Include
    @ReadPermission(expression = "user has all access AND (Allow OR user has all access)")
    static class GOOD10 {
    }

    @Entity
    @Include
    @ReadPermission(expression = "user has all access AND (user has all access OR user has all access)")
    static class GOOD8 {
    }

    @Entity
    @Include(rootLevel = true)
    @ReadPermission(expression = "owner is user AND user has all access")
    @CreatePermission(expression = "(Deny or Allow) and (not Deny)")
    @DeletePermission(expression = "user has all access or user has no access")
    @UpdatePermission(expression = "user has all access and (user has no access or user has all access)")
    public class Author {
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
    @ReadPermission(expression = "owner is user AND user has all access")
    @CreatePermission(expression = "(Deny or Allow) and (not Deny)")
    @DeletePermission(expression = "user has all access or user has no access")
    @UpdatePermission(expression = "user has all access and (user has no access or user has all access)")
    public class Book {
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
    public static class DummyExpression implements Expression {
        Check check;

        @Override
        public ExpressionResult evaluate() {
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

    public static Predicate createDummyPredicate() {
        List<Predicate.PathElement> pathList = new ArrayList<>();
        Predicate.PathElement path1 = new Predicate.PathElement(Author.class, "Author", Book.class, "books");
        Predicate.PathElement path2 = new Predicate.PathElement(Book.class, "Book", String.class, "title");
        pathList.add(path1);
        pathList.add(path2);
        Operator op = Operator.IN;
        List<Object> value = new ArrayList<>();
        value.add("Harry Potter");
        return new Predicate(pathList, op, value);
    }

    public static class FilterExpressionCheck1 extends FilterExpressionCheck {

        @Override
        public Predicate getFilterExpression(RequestScope requestScope) {
            return createDummyPredicate();
        }

        @Override
        public boolean ok(User user) {
            return true;
        }

        public FilterExpressionCheck1() {

        }
    }
}
