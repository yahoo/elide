/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.test.graphql.elements;

import java.io.Serializable;

/**
 * {@link Definition} represents the same concept as {@link graphql.language.Definition GraphQL Definition} but
 * specializes in serialization, in contrast to {@link graphql.language.Definition GraphQL Definition}, which is
 * designed for deserialization.
 */
public abstract class Definition implements Serializable {

    private static final long serialVersionUID = 9111832639983951228L;

    /**
     * Returns the query string that corresponds to the a {@link graphql.language.Definition} part
     * of a GraphQL query.
     *
     * @return a sub-string of a GraphQL query
     */
    abstract String toGraphQLSpec();

    /**
     * Returns the response string that corresponds to the a {@link graphql.language.Definition} part
     * of a GraphQL query.
     *
     * @return a sub-string of a GraphQL response
     */
    String toResponse() {
        throw new UnsupportedOperationException("Typed operation cannot be in response");
    }
}
