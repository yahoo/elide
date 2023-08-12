/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import com.google.common.collect.ImmutableMap;

import lombok.Getter;

import java.util.Arrays;
import java.util.function.Function;

/**
 * Key words used in graphql parsing.
 */
public enum KeyWord {
    NODE("node"),
    EDGES("edges"),
    PAGE_INFO("pageInfo"),
    PAGE_INFO_HAS_NEXT_PAGE("hasNextPage"),
    PAGE_INFO_START_CURSOR("startCursor"),
    PAGE_INFO_END_CURSOR("endCursor"),
    PAGE_INFO_TOTAL_RECORDS("totalRecords"),
    TYPENAME("__typename"),
    SCHEMA("__schema"),
    TYPE("__type"),
    UNKNOWN("unknown");

    private static final ImmutableMap<String, KeyWord> NAME_MAP =
            Arrays.stream(values()).collect(ImmutableMap.toImmutableMap(KeyWord::getName, Function.identity()));

    @Getter
    private final String name;

    KeyWord(String name) {
        this.name = name;
    }

    public boolean hasName(String name) {
        return this.name.equals(name);
    }

    public static KeyWord byName(String value) {
        return NAME_MAP.getOrDefault(value, UNKNOWN);
    }
}
