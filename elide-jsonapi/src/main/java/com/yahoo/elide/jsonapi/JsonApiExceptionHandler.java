/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi;

import com.yahoo.elide.core.exceptions.ExceptionHandler;

/**
 * JSON API exception handler.
 */
public interface JsonApiExceptionHandler extends ExceptionHandler<JsonApiErrorContext> {
}
