/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.models;

/**
 * ENUM of supported result types.
 */
public enum ResultType {
    JSON(FileExtensionType.JSON),
    CSV(FileExtensionType.CSV);

    private final FileExtensionType fileExtensionType;

    ResultType(FileExtensionType fileExtensionType) {
        this.fileExtensionType = fileExtensionType;
    }

    public FileExtensionType getFileExtensionType() {
        return fileExtensionType;
    }
}
