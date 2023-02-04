/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example.checks;

import static example.checks.InternalBookCheck.HIDDEN_BOOK;

import com.yahoo.elide.annotation.SecurityCheck;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.predicates.LEPredicate;
import com.yahoo.elide.core.security.RequestScope;
import com.yahoo.elide.core.security.checks.FilterExpressionCheck;
import com.yahoo.elide.core.type.Type;
import example.Book;

@SecurityCheck(HIDDEN_BOOK)
public class InternalBookCheck extends FilterExpressionCheck {

    public static final String HIDDEN_BOOK = "hidden book";

    @Override
    public FilterExpression getFilterExpression(Type entityClass, RequestScope requestScope) {
        return new LEPredicate(new Path.PathElement(Book.class, Long.class, "id"), 100);
    }
}
