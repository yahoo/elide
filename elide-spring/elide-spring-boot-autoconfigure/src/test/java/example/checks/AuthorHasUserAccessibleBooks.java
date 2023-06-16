/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.checks;

import com.yahoo.elide.annotation.SecurityCheck;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.security.RequestScope;
import com.yahoo.elide.core.security.checks.FilterExpressionCheck;
import com.yahoo.elide.core.type.Type;

import example.models.jpa.PermissionAuthor;
import example.models.jpa.PermissionAuthorBook;
import example.models.jpa.PermissionBook;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Check to test that relationship updates have inline checks deferred.
 */
@SecurityCheck("author has user accessible books")
public class AuthorHasUserAccessibleBooks extends FilterExpressionCheck<PermissionAuthor> {

    @Override
    public FilterExpression getFilterExpression(Type<?> entityClass, RequestScope requestScope) {
        Path.PathElement author = new Path.PathElement(PermissionAuthor.class, PermissionAuthorBook.class, "authorBooks");
        Path.PathElement book = new Path.PathElement(PermissionAuthorBook.class, PermissionBook.class, "book");
        Path.PathElement id = new Path.PathElement(PermissionBook.class, Long.class, "id");

        List<Path.PathElement> pathList = new ArrayList<>();
        pathList.add(author);
        pathList.add(book);
        pathList.add(id);
        Path paths = new Path(pathList);

        // For simplicity this hard codes the book id to 1 and 2 instead of retrieving from the principal
        return new FilterPredicate(paths, Operator.IN, Arrays.asList(1, 2));
    }
}
