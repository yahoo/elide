/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.jsonformats;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.processing.Processor;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.keyword.validator.AbstractKeywordValidator;
import com.github.fge.jsonschema.processors.data.FullData;
import com.github.fge.msgsimple.bundle.MessageBundle;
import com.google.common.collect.Sets;

import java.util.Set;

public class EitherTableSourceOrValuesValidator extends AbstractKeywordValidator {

    public static final String KEYWORD = "eitherTableSourceOrValues";
    public static final String ERROR_KEY = "eitherTableSourceOrValues.error";
    public static final String ERROR_MSG =
                    "Either tableSource or values should be defined for a dimension, Both are not allowed.";

    public EitherTableSourceOrValuesValidator(final JsonNode digest) {
        super(KEYWORD);
    }

    @Override
    public void validate(Processor<FullData, FullData> processor, ProcessingReport report, MessageBundle bundle,
                    FullData data) throws ProcessingException {

        JsonNode instance = data.getInstance().getNode();
        Set<String> fields = Sets.newHashSet(instance.fieldNames());

        if (fields.contains("values") && fields.contains("tableSource")) {
            report.error(newMsg(data, bundle, ERROR_KEY));
        }
    }

    @Override
    public String toString() {
        return "";
    }
}
