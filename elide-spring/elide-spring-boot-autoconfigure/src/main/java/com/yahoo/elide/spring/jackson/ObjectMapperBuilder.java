/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.jackson;

import tools.jackson.databind.ObjectMapper;

/**
 * Used to build an ObjectMapper.
 *
 * @see tools.jackson.databind.ObjectMapper
 */
@FunctionalInterface
public interface ObjectMapperBuilder {
    ObjectMapper build();
}
