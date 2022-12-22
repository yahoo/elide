/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.containers;

import com.yahoo.elide.graphql.Environment;

/**
 * Interface describing how to process GraphQL request at each step.
 * @param <T> The type returned by the container.
 */
public interface GraphQLContainer<T> {
    T processFetch(Environment context);
}
