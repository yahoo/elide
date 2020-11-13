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

public class ElideTimeFieldTypeFormatAttr extends AbstractFormatAttribute {
    private static final FormatAttribute INSTANCE = new ElideTimeFieldTypeFormatAttr();
    private static final String TIME_FIELD_TYPE_REGEX = "^(?i)(Time)$";

    public static final String FORMAT_NAME = "elideTimeFieldType";
    public static final String TYPE_KEY = "elideTimeFieldType.error.enum";
    public static final String TYPE_MSG = "Field type [%s] is not allowed. Field type must be "
                    + "[Time] for any time dimension.";

    private ElideTimeFieldTypeFormatAttr() {
        super(FORMAT_NAME, NodeType.STRING);
    }

    public static FormatAttribute getInstance() {
        return INSTANCE;
    }

    @Override
    public void validate(final ProcessingReport report, final MessageBundle bundle, final FullData data)
                    throws ProcessingException {
        final String input = data.getInstance().getNode().textValue();

        if (!input.matches(TIME_FIELD_TYPE_REGEX)) {
            report.error(newMsg(data, bundle, TYPE_KEY).putArgument("value", input));
        }
    }
}
