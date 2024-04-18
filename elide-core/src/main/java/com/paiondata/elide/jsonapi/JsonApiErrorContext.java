/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi;

import com.paiondata.elide.core.exceptions.ErrorContext;

import lombok.Builder;
import lombok.Getter;

/**
 * JsonApiErrorContext.
 */
@Builder
@Getter
public class JsonApiErrorContext implements ErrorContext {
    private final boolean verbose;
    private final JsonApiMapper mapper;
}
