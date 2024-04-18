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

/**
 * Format specifier for {@code elideJdbcUrl} format attribute.
 * <p>
 * This specifier will check if a string instance is a valid JDBC url.
 * </p>
 */
public class ElideJDBCUrlFormatAttr extends AbstractFormatAttribute {

    public static final String FORMAT_NAME = "elideJdbcUrl";
    public static final String FORMAT_KEY = "elideJdbcUrl.error.format";
    public static final String FORMAT_MSG = "Input value [%s] is not a valid JDBC url, it must start with 'jdbc:'.";

    public ElideJDBCUrlFormatAttr() {
        super(FORMAT_NAME, NodeType.STRING);
    }

    @Override
    public void validate(final ProcessingReport report, final MessageBundle bundle, final FullData data)
                    throws ProcessingException {
        final String input = data.getInstance().getNode().textValue();

        if (!input.startsWith("jdbc:")) {
            report.error(newMsg(data, bundle, FORMAT_KEY).putArgument("value", input));
        }
    }
}
