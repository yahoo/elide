/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.testhelpers.example;

import com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL;
import com.yahoo.elide.contrib.testhelpers.graphql.elements.ObjectValueWithVariableTest;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * A test bean for {@link ObjectValueWithVariableTest}
 * <p>
 * <b>CAUTION: DO NOT DECORATE IT WITH {@link Builder}, which hides its no-args constructor. This will result in
 * runtime error at places such as {@code entityClass.newInstance();}</b>
 */
public class Author {

    /**
     * A test field.
     */
    @Getter
    @Setter
    @JsonProperty
    private Long id;

    /**
     * A test field.
     */
    @Getter
    @Setter
    @JsonProperty
    private String name;
}
