/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Used to store the singleton shared {@link ObjectMapper} instance.
 */
@Getter
@AllArgsConstructor
public class ElideMapper {
    private final ObjectMapper objectMapper;
}
