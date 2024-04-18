/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.graphql;

import com.paiondata.elide.core.exceptions.ErrorContext;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Builder;
import lombok.Getter;

/**
 * GraphQLErrorContext.
 */
@Builder
@Getter
public class GraphQLErrorContext implements ErrorContext {
    private final boolean verbose;
    private final ObjectMapper objectMapper;
    private final String graphQLDocument;
}
