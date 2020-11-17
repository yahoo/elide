/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.jsonformats;

import com.github.fge.jackson.NodeType;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.format.AbstractFormatAttribute;
import com.github.fge.jsonschema.format.FormatAttribute;
import com.github.fge.jsonschema.processors.data.FullData;
import com.github.fge.msgsimple.bundle.MessageBundle;

public class ElideNameFormatAttr extends AbstractFormatAttribute {
    private static final FormatAttribute INSTANCE = new ElideNameFormatAttr();
    private static final String NAME_FORMAT_REGEX = "^[A-Za-z][0-9A-Za-z_]*$";

    public static final String FORMAT_NAME = "elideName";
    public static final String FORMAT_KEY = "elideName.error.format";
    public static final String FORMAT_MSG =
                    "Name [%s] is not allowed. Name must start with an alphabet and can include "
                    + "alaphabets, numbers and '_' only.";

    private ElideNameFormatAttr() {
        super(FORMAT_NAME, NodeType.STRING);
    }

    public static FormatAttribute getInstance() {
        return INSTANCE;
    }

    @Override
    public void validate(final ProcessingReport report, final MessageBundle bundle, final FullData data)
                    throws ProcessingException {
        final String input = data.getInstance().getNode().textValue();

        if (!input.matches(NAME_FORMAT_REGEX)) {
            report.error(newMsg(data, bundle, FORMAT_KEY).putArgument("value", input));
        }
    }
}
