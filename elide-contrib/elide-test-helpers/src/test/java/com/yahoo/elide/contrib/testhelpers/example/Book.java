/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.testhelpers.example;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Singular;

import java.util.ArrayList;
import java.util.Collection;

@Builder
public class Book {

    @JsonProperty
    private long id;

    @JsonProperty
    private String title;

    @Singular
    @JsonProperty
    private Collection<Author> authors = new ArrayList<>();
}
