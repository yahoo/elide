/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig;


import com.networknt.schema.ValidationMessage;

import java.io.IOException;
import java.util.Set;

/**
 * Indicates that the schema is not valid.
 */
public class InvalidSchemaException extends IOException {

    private static final long serialVersionUID = 1L;

    private final Set<ValidationMessage> validationMessages;
    private final String fileName;

    public InvalidSchemaException(String fileName, Set<ValidationMessage> validationMessages) {
        super("Schema validation failed for: " + fileName + ValidationMessages.toString(validationMessages));
        this.fileName = fileName;
        this.validationMessages = validationMessages;
    }

    public Set<ValidationMessage> getValidationMessages() {
        return validationMessages;
    }

    public String getFileName() {
        return fileName;
    }
}
