/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.checks;

import com.paiondata.elide.annotation.SecurityCheck;
import com.paiondata.elide.core.Path;
import com.paiondata.elide.core.filter.expression.FilterExpression;
import com.paiondata.elide.core.filter.predicates.PostfixPredicate;
import com.paiondata.elide.core.security.RequestScope;
import com.paiondata.elide.core.security.checks.FilterExpressionCheck;
import com.paiondata.elide.core.type.Type;
import example.VideoGame;

/**
 * Filter Expression Check for video game.
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
