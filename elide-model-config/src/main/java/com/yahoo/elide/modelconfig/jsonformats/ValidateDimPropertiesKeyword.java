/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.jsonformats;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.NodeType;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.keyword.syntax.checkers.AbstractSyntaxChecker;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.core.tree.SchemaTree;
import com.github.fge.jsonschema.keyword.digest.AbstractDigester;
import com.github.fge.jsonschema.library.Keyword;
import com.github.fge.msgsimple.bundle.MessageBundle;

import lombok.Getter;

import java.util.Collection;

/**
 * Creates custom keyword for {@code validateDimensionProperties}.
 */
public class ValidateDimPropertiesKeyword {

    @Getter
    private Keyword keyword;

    public ValidateDimPropertiesKeyword() {
        keyword = Keyword.newBuilder(ValidateDimPropertiesValidator.KEYWORD)
                        .withSyntaxChecker(new ValidateDimPropertiesSyntaxChecker())
                        .withDigester(new ValidateDimPropertiesDigester())
                        .withValidatorClass(ValidateDimPropertiesValidator.class)
                        .freeze();
    }

    /**
     * Defines custom SyntaxChecker for {@code validateDimensionProperties}.
     */
    private class ValidateDimPropertiesSyntaxChecker extends AbstractSyntaxChecker {

        public ValidateDimPropertiesSyntaxChecker() {
            super(ValidateDimPropertiesValidator.KEYWORD, NodeType.BOOLEAN);
        }

        @Override
        protected void checkValue(Collection<JsonPointer> pointers, MessageBundle bundle, ProcessingReport report,
                        SchemaTree tree) throws ProcessingException {
            // AbstractSyntaxChecker has already verified that value is of type Boolean
            // No additional Checks Required
        }
    }

    /**
     * Defines custom Digester for {@code validateDimensionProperties}.
     */
    private class ValidateDimPropertiesDigester extends AbstractDigester {

        public ValidateDimPropertiesDigester() {
            super(ValidateDimPropertiesValidator.KEYWORD, NodeType.OBJECT);
        }

        @Override
        public JsonNode digest(final JsonNode schema) {
            final ObjectNode node = FACTORY.objectNode();
            node.put(keyword, schema.get(keyword).asBoolean(true));
            return node;
        }
    }
}
