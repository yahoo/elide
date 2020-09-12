/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.async.models;

import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

/**
 * ENUM of supported result format types.
 * ex. JSON, CSV etc.
 */
public enum ResultFormatType {
    DEFAULT(EnumSet.of(ResultType.EMBEDDED)), // Default JSONAPI, GRAPHQLAPI based on QueryType
    //TODO - Add JSON(ResultType.EMBEDDED, ResultType.DOWNLOAD),
    CSV(EnumSet.of(ResultType.EMBEDDED, ResultType.DOWNLOAD));

    @Getter private final Set<ResultType> supportedResultType;

    private ResultFormatType(Set<ResultType> supportedResultType) {
        this.supportedResultType = supportedResultType;
    }

    public boolean supportsDownload() {
        return this.supportedResultType.contains(ResultType.DOWNLOAD);
    }
}
