/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.async.models;

import lombok.Getter;

public enum ResultFormatType {
    JSONAPI(false),
    GRAPHQLAPI(false),
    //TODO - Add JSON(true),
    CSV(true);

    @Getter private final boolean supportsDownload;

    private ResultFormatType(boolean supportsDownload) {
        this.supportsDownload = supportsDownload;
    }
}
