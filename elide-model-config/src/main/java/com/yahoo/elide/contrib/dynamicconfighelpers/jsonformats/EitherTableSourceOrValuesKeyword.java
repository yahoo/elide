/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.jsonformats;

import com.fasterxml.jackson.databind.JsonNode;
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

public class EitherTableSourceOrValuesKeyword {

    @Getter
    private Keyword keyword;

    public EitherTableSourceOrValuesKeyword() {
        keyword = Keyword.newBuilder(EitherTableSourceOrValuesValidator.KEYWORD)
                        .withSyntaxChecker(new EitherTableSourceOrValuesSyntaxChecker())
                        .withDigester(new EitherTableSourceOrValuesDigesters())
                        .withValidatorClass(EitherTableSourceOrValuesValidator.class)
                        .freeze();
    }

    private class EitherTableSourceOrValuesSyntaxChecker extends AbstractSyntaxChecker {

        public EitherTableSourceOrValuesSyntaxChecker() {
            super(EitherTableSourceOrValuesValidator.KEYWORD, NodeType.OBJECT);
        }

        @Override
        protected void checkValue(Collection<JsonPointer> pointers, MessageBundle bundle, ProcessingReport report,
                        SchemaTree tree) throws ProcessingException {
            // No Checks Required
        }
    }

    private class EitherTableSourceOrValuesDigesters extends AbstractDigester {

        public EitherTableSourceOrValuesDigesters() {
            super(EitherTableSourceOrValuesValidator.KEYWORD, NodeType.OBJECT);
        }

        @Override
        public JsonNode digest(final JsonNode schema) {
            // No Digest Required
            return FACTORY.objectNode();
        }
    }
}
