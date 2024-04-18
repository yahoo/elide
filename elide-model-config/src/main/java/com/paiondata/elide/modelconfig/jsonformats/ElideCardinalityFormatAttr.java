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
 * Format specifier for {@code elideCardiality} format attribute.
 * <p>
 * This specifier will check if a string instance is one of {@code Tiny, Small, Medium, Large, Huge}.
 * </p>
 */
public class ElideCardinalityFormatAttr extends AbstractFormatAttribute {
    private static final Pattern CARDINALITY_PATTERN = Pattern.compile("^(?i)(Tiny|Small|Medium|Large|Huge)$");

    public static final String FORMAT_NAME = "elideCardiality";
    public static final String TYPE_KEY = "elideCardiality.error.enum";
    public static final String TYPE_MSG = "Cardinality type [%s] is not allowed. Supported value is one of "
                    + "[Tiny, Small, Medium, Large, Huge].";

    public ElideCardinalityFormatAttr() {
        super(FORMAT_NAME, NodeType.STRING);
    }

    @Override
    public void validate(final ProcessingReport report, final MessageBundle bundle, final FullData data)
                    throws ProcessingException {
        final String input = data.getInstance().getNode().textValue();

        if (!CARDINALITY_PATTERN.matcher(input).matches()) {
            report.error(newMsg(data, bundle, TYPE_KEY).putArgument("value", input));
        }
    }
}
