/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig;


import com.networknt.schema.Error;

import java.io.IOException;
import java.util.List;

/**
 * Indicates that the schema is not valid.
 */
public class InvalidSchemaException extends IOException {

    private static final long serialVersionUID = 1L;

    private final List<Error> validationMessages;
    private final String fileName;

    public InvalidSchemaException(String fileName, List<Error> validationMessages) {
        super("Schema validation failed for: " + fileName + ValidationErrors.toString(validationMessages));
        this.fileName = fileName;
        this.validationMessages = validationMessages;
    }

    public List<Error> getValidationMessages() {
        return validationMessages;
    }

    public String getFileName() {
        return fileName;
    }
}
