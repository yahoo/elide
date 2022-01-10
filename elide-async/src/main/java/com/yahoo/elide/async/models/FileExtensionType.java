/*
 * Copyright 2022, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.async.models;

/**
 * ENUM of supported file extension types.
 */
public enum FileExtensionType {
    JSON(".json"),
    CSV(".csv"),
    NONE("");

    private final String extension;

    FileExtensionType(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }
}
