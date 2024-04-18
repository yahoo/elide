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
 * Format specifier for {@code elideGrainType} format attribute.
 * <p>
 * This specifier will check if a string instance is one of
 * {@code Second, Minute, Hour, Day, IsoWeek, Week, Month, Quarter, Year}.
 * </p>
 */
public class ElideGrainTypeFormatAttr extends AbstractFormatAttribute {
    private static final Pattern GRAIN_TYPE_PATTERN =
            Pattern.compile("^(?i)(Second|Minute|Hour|Day|IsoWeek|Week|Month|Quarter|Year)$");

    public static final String FORMAT_NAME = "elideGrainType";
    public static final String TYPE_KEY = "elideGrainType.error.enum";
    public static final String TYPE_MSG = "Grain type [%s] is not allowed. Supported value is one of "
            + "[Second, Minute, Hour, Day, IsoWeek, Week, Month, Quarter, Year].";

    public ElideGrainTypeFormatAttr() {
        super(FORMAT_NAME, NodeType.STRING);
    }

    @Override
    public void validate(final ProcessingReport report, final MessageBundle bundle, final FullData data)
            throws ProcessingException {
        final String input = data.getInstance().getNode().textValue();

        if (!GRAIN_TYPE_PATTERN.matcher(input).matches()) {
            report.error(newMsg(data, bundle, TYPE_KEY).putArgument("value", input));
        }
    }
}
