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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Defines custom Keyword Validator for {@code validateDimensionProperties}.
 * <p>
 * This validator checks neither additional properties are defined for any dimension nor not both {@code tableSource}
 * and {@code values} property is defined for any dimension.
 * </p>
 */
public class ValidateDimPropertiesValidator extends AbstractKeywordValidator {

    private static final Set<String> ALL_DIM_PROPERTIES = new HashSet<>(Arrays.asList("name", "description", "category",
                    "hidden", "readAccess", "definition", "cardinality", "tags", "type", "values", "tableSource"));

    public static final String KEYWORD = "validateDimensionProperties";
    public static final String ATMOST_ONE_KEY = "validateDimensionProperties.error.atmostOne";
    public static final String ATMOST_ONE_MSG =
                    "tableSource and values cannot both be defined for a dimension. Choose One or None.";
    public static final String ADDITIONAL_KEY = "validateDimensionProperties.error.addtional";
    public static final String ADDITIONAL_MSG = "Properties: %s are not allowed for dimensions.";

    private boolean validate;

    public ValidateDimPropertiesValidator(final JsonNode digest) {
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

            fields.removeAll(ALL_DIM_PROPERTIES);
            if (!fields.isEmpty()) {
                report.error(newMsg(data, bundle, ADDITIONAL_KEY).putArgument("value", fields.toString()));
            }
        }
    }

    @Override
    public String toString() {
        return keyword;
    }
}
