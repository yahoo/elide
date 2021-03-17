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
 * Format specifier for {@code elideName} format attribute.
 * <p>
 * This specifier will check if a string instance is a valid Elide Name.
 * </p>
 */
public class ElideNameFormatAttr extends AbstractFormatAttribute {
    public static final String NAME_FORMAT_REGEX = "^[A-Za-z][0-9A-Za-z_]*$";

    public static final String FORMAT_NAME = "elideName";
    public static final String NAME_KEY = "elideName.error.name";
    public static final String NAME_MSG = "Name [%s] is not allowed. Name cannot be 'table'";
    public static final String FORMAT_KEY = "elideName.error.format";
    public static final String FORMAT_MSG =
                    "Name [%s] is not allowed. Name must start with an alphabet and can include "
                    + "alaphabets, numbers and '_' only.";

    public ElideNameFormatAttr() {
        super(FORMAT_NAME, NodeType.STRING);
    }

    @Override
    public void validate(final ProcessingReport report, final MessageBundle bundle, final FullData data)
                    throws ProcessingException {
        final String input = data.getInstance().getNode().textValue();

        if (!input.matches(NAME_FORMAT_REGEX)) {
            report.error(newMsg(data, bundle, FORMAT_KEY).putArgument("value", input));
        }

        if (input.equalsIgnoreCase("table")) {
            report.error(newMsg(data, bundle, NAME_KEY).putArgument("value", input));
        }
    }
}
