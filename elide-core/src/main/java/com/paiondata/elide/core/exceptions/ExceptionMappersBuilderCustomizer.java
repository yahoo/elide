/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.exceptions;

import com.paiondata.elide.core.exceptions.ExceptionMappers.ExceptionMappersBuilder;

/**
 * Customizer for {@link ExceptionMappersBuilder}.
 */
@FunctionalInterface
public interface ExceptionMappersBuilderCustomizer {
    /**
     * Customize the {@link ExceptionMapperBuilder}.
     *
     * @param exceptionMapperBuilder the builder
     */
    void customize(ExceptionMappersBuilder exceptionMappersBuilder);
}
