/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.yahoo.elide.core.exceptions.ExceptionHandler;

/**
 * GraphQL exception handler.
 */
public interface GraphQLExceptionHandler extends ExceptionHandler<GraphQLErrorContext> {
}
