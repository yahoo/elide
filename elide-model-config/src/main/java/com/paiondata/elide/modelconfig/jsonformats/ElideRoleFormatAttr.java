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
 * Format specifier for {@code elideRole} format attribute.
 * <p>
 * This specifier will check if a string instance is a valid Elide role.
 * </p>
 */
public class ElideRoleFormatAttr extends AbstractFormatAttribute {
    private static final Pattern ROLE_FORMAT_REGEX = Pattern.compile("^[A-Za-z][0-9A-Za-z. ]*$");

    public static final String FORMAT_NAME = "elideRole";
    public static final String FORMAT_KEY = "elideRole.error.format";
    public static final String FORMAT_MSG =
                    "Role [%s] is not allowed. Role must start with an alphabetic character and can include "
                    + "alaphabets, numbers, spaces and '.' only.";

    public ElideRoleFormatAttr() {
        super(FORMAT_NAME, NodeType.STRING);
    }

    @Override
    public void validate(final ProcessingReport report, final MessageBundle bundle, final FullData data)
                    throws ProcessingException {
        final String input = data.getInstance().getNode().textValue();

        if (!ROLE_FORMAT_REGEX.matcher(input).matches()) {
            report.error(newMsg(data, bundle, FORMAT_KEY).putArgument("value", input));
        }
    }
}
