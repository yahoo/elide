/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.testhelpers.example;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;

@Builder
public class Author {

    @JsonProperty
    private Long id;

    @JsonProperty
    private String name;
}
