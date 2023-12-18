/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.parser;

import com.yahoo.elide.generated.parsers.CoreParser.QueryContext;
import com.yahoo.elide.jsonapi.JsonApiRequestScope;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import org.apache.commons.lang3.tuple.Pair;

import java.util.function.Supplier;

/**
 * POST handler.
 */
public class PostVisitor extends BaseVisitor {

    /**
     * Constructor.
     *
     * @param requestScope the request scope
     */
    public PostVisitor(JsonApiRequestScope requestScope) {
        super(requestScope);
    }

    @Override
    public Supplier<Pair<Integer, JsonApiDocument>> visitQuery(QueryContext ctx) {
        return state.handlePost();
    }
}
