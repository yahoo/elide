/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.modelconfig.jsonformats;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.processing.Processor;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.keyword.validator.AbstractKeywordValidator;
import com.github.fge.jsonschema.processors.data.FullData;
import com.github.fge.msgsimple.bundle.MessageBundle;
import com.google.common.collect.Sets;

import java.util.Set;

/**
 * Defines custom Keyword Validator for {@code validateArgumentProperties}.
 * <p>
 * This validator checks not both {@code tableSource} and {@code values} property is defined for any argument.
 * </p>
 */
public class ValidateArgsPropertiesValidator extends AbstractKeywordValidator {

    public static final String KEYWORD = "validateArgumentProperties";
    public static final String ATMOST_ONE_KEY = "validateArgumentProperties.error.atmostOne";
    public static final String ATMOST_ONE_MSG =
                    "tableSource and values cannot both be defined for an argument. Choose One or None.";

    private boolean validate;

    public ValidateArgsPropertiesValidator(final JsonNode digest) {
        super(KEYWORD);
        validate = digest.get(keyword).booleanValue();
    }

    @Override
    public void validate(Processor<FullData, FullData> processor, ProcessingReport report, MessageBundle bundle,
                    FullData data) throws ProcessingException {

        if (validate) {
            JsonNode instance = data.getInstance().getNode();
            Set<String> fields = Sets.newHashSet(instance.fieldNames());

            if (fields.contains("values") && fields.contains("tableSource")) {
                report.error(newMsg(data, bundle, ATMOST_ONE_KEY));
            }

        }
    }

    @Override
    public String toString() {
        return keyword;
    }
}
