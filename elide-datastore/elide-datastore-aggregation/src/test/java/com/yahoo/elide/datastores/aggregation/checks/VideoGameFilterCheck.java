/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.checks;

import com.yahoo.elide.annotation.SecurityCheck;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.predicates.PostfixPredicate;
import com.yahoo.elide.core.security.RequestScope;
import com.yahoo.elide.core.security.checks.FilterExpressionCheck;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.example.VideoGame;

/**
 * Filter Expression Check for video game
 */
@SecurityCheck(VideoGameFilterCheck.NAME_FILTER)
public class VideoGameFilterCheck extends FilterExpressionCheck<VideoGame> {
    public static final String NAME_FILTER = "player name filter";
    @Override
    public FilterExpression getFilterExpression(Type<?> entityClass, RequestScope requestScope) {
        Path path = super.getFieldPath(entityClass, requestScope, "getPlayerName", "playerName");
        return new PostfixPredicate(path, "Doe");
    }
}
