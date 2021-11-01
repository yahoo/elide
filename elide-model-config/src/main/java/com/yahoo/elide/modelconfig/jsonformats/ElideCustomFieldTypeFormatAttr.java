/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.jsonformats;

import static com.yahoo.elide.modelconfig.jsonformats.JavaClassNameFormatAttr.CLASS_NAME_FORMAT_PATTERN;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.processors.data.FullData;
import com.github.fge.msgsimple.bundle.MessageBundle;

/**
 * Format specifier for {@code elideCustomFieldType} format attribute.
 * <p>
 * This specifier will check if a string instance is one of {@code Integer, Decimal, Money, Text, Coordinate, Boolean}
 * or a custom class type.
 * </p>
 */
public class ElideCustomFieldTypeFormatAttr extends ElideFieldTypeFormatAttr {

    public static final String FORMAT_NAME = "elideCustomFieldType";
    public static final String TYPE_KEY = "elideCustomFieldType.error.enum";
    public static final String TYPE_MSG = "Field type [%s] is not allowed. Supported value is one of "
            + "[Integer, Decimal, Money, Text, Coordinate, Boolean] or a valid Java class name.";

    public ElideCustomFieldTypeFormatAttr() {
        super(FORMAT_NAME);
    }

    @Override
    public void validate(final ProcessingReport report, final MessageBundle bundle, final FullData data)
            throws ProcessingException {
        final String input = data.getInstance().getNode().textValue();

        boolean matches = false;
        if (!FIELD_TYPE_PATTERN.matcher(input).matches()) {
            if (CLASS_NAME_FORMAT_PATTERN.matcher(input).matches()) {
                try {
                    Class.forName(input);
                    matches = true;
                } catch (ClassNotFoundException e) {

                }
            }
        } else {
            matches = true;
        }

        if (!matches) {
            report.error(newMsg(data, bundle, TYPE_KEY).putArgument("value", input));
        }
    }
}
