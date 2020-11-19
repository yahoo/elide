/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.jsonformats;

import com.github.fge.jackson.NodeType;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.format.AbstractFormatAttribute;
import com.github.fge.jsonschema.processors.data.FullData;
import com.github.fge.msgsimple.bundle.MessageBundle;

/**
 * Format specifier for {@code elideFieldType} format attribute.
 * <p>
 * This specifier will check if a string instance is one of {@code Integer, Decimal, Money, Text, Coordinate, Boolean}.
 * </p>
 */
public class ElideFieldTypeFormatAttr extends AbstractFormatAttribute {
    private static final String FIELD_TYPE_REGEX = "^(?i)(Integer|Decimal|Money|Text|Coordinate|Boolean)$";

    public static final String FORMAT_NAME = "elideFieldType";
    public static final String TYPE_KEY = "elideFieldType.error.enum";
    public static final String TYPE_MSG = "Field type [%s] is not allowed. Supported value is one of "
                    + "[Integer, Decimal, Money, Text, Coordinate, Boolean].";

    public ElideFieldTypeFormatAttr() {
        super(FORMAT_NAME, NodeType.STRING);
    }

    @Override
    public void validate(final ProcessingReport report, final MessageBundle bundle, final FullData data)
                    throws ProcessingException {
        final String input = data.getInstance().getNode().textValue();

        if (!input.matches(FIELD_TYPE_REGEX)) {
            report.error(newMsg(data, bundle, TYPE_KEY).putArgument("value", input));
        }
    }
}
