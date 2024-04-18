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

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Format specifier for {@code elideNamespaceName} format attribute.
 * <p>
 * This specifier will check if a string instance is a valid Elide Name.
 * </p>
 */
public class ElideNamespaceNameFormatAttr extends AbstractFormatAttribute {
    public static final Pattern NAME_FORMAT_REGEX = ElideNameFormatAttr.NAME_FORMAT_REGEX;

    public static final String FORMAT_NAME = "elideNamespaceName";
    public static final String FORMAT_KEY = "elideNamespaceName.error.format";
    public static final String FORMAT_ADDITIONAL_KEY = "elideNamespaceName.error.additional";
    public static final String FORMAT_KEY_MSG =
                    "Name [%s] is not allowed. Name must start with an alphabetic character and can include "
                    + "alaphabets, numbers and '_' only.";
    public static final String FORMAT_ADDITIONAL_KEY_MSG =
            "Name [%s] clashes with the 'default' namespace. Either change the case or pick a different namespace "
            + "name.";
    public static final String DEFAULT_NAME = "default";

    public ElideNamespaceNameFormatAttr() {
        super(FORMAT_NAME, NodeType.STRING);
    }

    @Override
    public void validate(final ProcessingReport report, final MessageBundle bundle, final FullData data)
                    throws ProcessingException {
        final String input = data.getInstance().getNode().textValue();

        if (!NAME_FORMAT_REGEX.matcher(input).matches()) {
            report.error(newMsg(data, bundle, FORMAT_KEY).putArgument("value", input));
        }

        if (!input.equals(DEFAULT_NAME) && input.toLowerCase(Locale.ENGLISH).equals(DEFAULT_NAME)) {
            report.error(newMsg(data, bundle, FORMAT_ADDITIONAL_KEY).putArgument("value", input));
        }
    }
}
