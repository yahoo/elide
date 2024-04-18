/*
 * Copyright 2021, Yahoo Inc.
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

/**
 * Format specifier for {@code elideArgumentName} format attribute.
 * <p>
 * This specifier will check if a string instance is a valid Elide Argument Name.
 * </p>
 */
public class ElideArgumentNameFormatAttr extends AbstractFormatAttribute {

    public static final String FORMAT_NAME = "elideArgumentName";
    public static final String NAME_KEY = "elideArgumentName.error.name";
    public static final String NAME_MSG = "Argument name [%s] is not allowed. Argument name cannot be 'grain'.";
    public static final String FORMAT_KEY = "elideArgumentName.error.format";
    public static final String FORMAT_MSG = "Argument name [%s] is not allowed. Name must start with an alphabetic "
                    + "character and can include alaphabets, numbers and '_' only.";

    public ElideArgumentNameFormatAttr() {
        super(FORMAT_NAME, NodeType.STRING);
    }

    @Override
    public void validate(final ProcessingReport report, final MessageBundle bundle, final FullData data)
                    throws ProcessingException {
        final String input = data.getInstance().getNode().textValue();

        if (!ElideNameFormatAttr.NAME_FORMAT_REGEX.matcher(input).matches()) {
            report.error(newMsg(data, bundle, FORMAT_KEY).putArgument("value", input));
        }

        if (input.equalsIgnoreCase("grain")) {
            report.error(newMsg(data, bundle, NAME_KEY).putArgument("value", input));
        }
    }
}
