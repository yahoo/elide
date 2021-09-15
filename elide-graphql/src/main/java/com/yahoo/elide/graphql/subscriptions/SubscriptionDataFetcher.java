/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.subscriptions;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import java.util.Map;

public class SubscriptionDataFetcher implements DataFetcher<Object> {
    @Override
    public Object get(DataFetchingEnvironment environment) throws Exception {
        /* fetch arguments in mutation/query */
        Map<String, Object> args = environment.getArguments();

        return null;
    }
}
