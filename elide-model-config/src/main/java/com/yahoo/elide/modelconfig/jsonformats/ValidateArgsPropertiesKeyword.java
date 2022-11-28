/*
 * Copyright 2021, Yahoo Inc.
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
 * Creates custom keyword for {@code validateArgumentProperties}.
 */
public class ValidateArgsPropertiesKeyword {

    @Getter
    private Keyword keyword;

    public ValidateArgsPropertiesKeyword() {
        keyword = Keyword.newBuilder(ValidateArgsPropertiesValidator.KEYWORD)
                        .withSyntaxChecker(new ValidateArgsPropertiesSyntaxChecker())
                        .withDigester(new ValidateArgsPropertiesDigester())
                        .withValidatorClass(ValidateArgsPropertiesValidator.class)
                        .freeze();
    }

    /**
     * Defines custom SyntaxChecker for {@code validateArgumentProperties}.
     */
    private class ValidateArgsPropertiesSyntaxChecker extends AbstractSyntaxChecker {

        public ValidateArgsPropertiesSyntaxChecker() {
            super(ValidateArgsPropertiesValidator.KEYWORD, NodeType.BOOLEAN);
        }

        @Override
        protected void checkValue(Collection<JsonPointer> pointers, MessageBundle bundle, ProcessingReport report,
                        SchemaTree tree) throws ProcessingException {
            // AbstractSyntaxChecker has already verified that value is of type Boolean
            // No additional Checks Required
        }
    }

    /**
     * Defines custom Digester for {@code validateArgumentProperties}.
     */
    private class ValidateArgsPropertiesDigester extends AbstractDigester {

        public ValidateArgsPropertiesDigester() {
            super(ValidateArgsPropertiesValidator.KEYWORD, NodeType.OBJECT);
        }

        @Override
        public JsonNode digest(final JsonNode schema) {
            final ObjectNode node = FACTORY.objectNode();
            node.put(keyword, schema.get(keyword).asBoolean(true));

            return node;
        }
    }
}
