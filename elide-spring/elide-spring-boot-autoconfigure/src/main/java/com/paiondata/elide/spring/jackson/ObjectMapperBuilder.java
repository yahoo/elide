/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Used to build an ObjectMapper.
 *
 * @see com.fasterxml.jackson.databind.ObjectMapper
 */
@FunctionalInterface
public interface ObjectMapperBuilder {
    ObjectMapper build();
}
