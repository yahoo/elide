/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.modelconfig.jsonformats;

import com.github.fge.jackson.NodeType;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.format.AbstractFormatAttribute;
import com.github.fge.jsonschema.processors.data.FullData;
import com.github.fge.msgsimple.bundle.MessageBundle;

import java.util.regex.Pattern;

/**
 * Format specifier for {@code elideFieldType} format attribute.
 * <p>
 * This specifier will check if a string instance is one of {@code Integer, Decimal, Money, Text, Coordinate, Boolean}.
 * </p>
 */
public class ElideFieldTypeFormatAttr extends AbstractFormatAttribute {
    public static final Pattern FIELD_TYPE_PATTERN =
            Pattern.compile("^(?i)(Integer|Decimal|Money|Text|Coordinate|Boolean|Enum_Text|Enum_Ordinal)$");

    public static final String FORMAT_NAME = "elideFieldType";
    public static final String TYPE_KEY = "elideFieldType.error.enum";
    public static final String TYPE_MSG = "Field type [%s] is not allowed. Supported value is one of "
            + "[Integer, Decimal, Money, Text, Coordinate, Boolean, Enum_Text, Enum_Ordinal].";

    public ElideFieldTypeFormatAttr() {
        this(FORMAT_NAME);
    }

    public ElideFieldTypeFormatAttr(String formatName) {
        super(formatName, NodeType.STRING);
    }

    @Override
    public void validate(final ProcessingReport report, final MessageBundle bundle, final FullData data)
            throws ProcessingException {
        final String input = data.getInstance().getNode().textValue();

        if (!FIELD_TYPE_PATTERN.matcher(input).matches()) {
            report.error(newMsg(data, bundle, TYPE_KEY).putArgument("value", input));
        }
    }
}
