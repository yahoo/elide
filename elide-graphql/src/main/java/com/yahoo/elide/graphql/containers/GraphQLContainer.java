/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.containers;

import com.yahoo.elide.graphql.ElideDataFetcher;
import com.yahoo.elide.graphql.Environment;

/**
 * Interface describing how to process GraphQL request at each step.
 */
public interface GraphQLContainer {
    Object processFetch(Environment context, ElideDataFetcher fetcher);
}
