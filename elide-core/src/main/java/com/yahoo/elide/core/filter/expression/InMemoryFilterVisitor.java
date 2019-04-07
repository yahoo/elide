/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.expression;

import com.yahoo.elide.core.RequestScope;

/**
 * Deprecated visitor for in memory filterExpressions.
 * @deprecated use {@link InMemoryFilterExecutor}
 */
@Deprecated
public class InMemoryFilterVisitor extends InMemoryFilterExecutor {

    public InMemoryFilterVisitor(RequestScope requestScope) {
        super(requestScope);
    }
}
