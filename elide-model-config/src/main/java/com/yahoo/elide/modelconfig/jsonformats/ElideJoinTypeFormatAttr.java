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
 * Format specifier for {@code elideJoinType} format attribute.
 * <p>
 * This specifier will check if a string instance is one of {@code ToOne, ToMany}.
 * </p>
 */
public class ElideJoinTypeFormatAttr extends AbstractFormatAttribute {
    private static final String JOIN_TYPE_REGEX = "^(?i)(ToOne|ToMany)$";

    public static final String FORMAT_NAME = "elideJoinType";
    public static final String TYPE_KEY = "elideJoinType.error.enum";
    public static final String TYPE_MSG = "Join type [%s] is not allowed. Supported value is one of [ToOne, ToMany].";

    public ElideJoinTypeFormatAttr() {
        super(FORMAT_NAME, NodeType.STRING);
    }

    @Override
    public void validate(final ProcessingReport report, final MessageBundle bundle, final FullData data)
                    throws ProcessingException {
        final String input = data.getInstance().getNode().textValue();

        if (!input.matches(JOIN_TYPE_REGEX)) {
            report.error(newMsg(data, bundle, TYPE_KEY).putArgument("value", input));
        }
    }
}
