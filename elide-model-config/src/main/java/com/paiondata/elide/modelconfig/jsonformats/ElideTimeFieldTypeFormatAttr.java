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
 * Format specifier for {@code elideTimeFieldType} format attribute.
 * <p>
 * This specifier will check if a string instance is {@code Time}.
 * </p>
 */
public class ElideTimeFieldTypeFormatAttr extends AbstractFormatAttribute {
    private static final Pattern TIME_FIELD_TYPE_PATTERN = Pattern.compile("^(?i)(Time)$");

    public static final String FORMAT_NAME = "elideTimeFieldType";
    public static final String TYPE_KEY = "elideTimeFieldType.error.enum";
    public static final String TYPE_MSG = "Field type [%s] is not allowed. Field type must be "
                    + "[Time] for any time dimension.";

    public ElideTimeFieldTypeFormatAttr() {
        super(FORMAT_NAME, NodeType.STRING);
    }

    @Override
    public void validate(final ProcessingReport report, final MessageBundle bundle, final FullData data)
                    throws ProcessingException {
        final String input = data.getInstance().getNode().textValue();

        if (!TIME_FIELD_TYPE_PATTERN.matcher(input).matches()) {
            report.error(newMsg(data, bundle, TYPE_KEY).putArgument("value", input));
        }
    }
}
