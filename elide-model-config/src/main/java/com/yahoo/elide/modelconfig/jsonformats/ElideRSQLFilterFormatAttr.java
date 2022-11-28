/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.jsonformats;

import static com.yahoo.elide.core.filter.dialect.RSQLFilterDialect.getDefaultOperatorsWithIsnull;

import com.github.fge.jackson.NodeType;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.format.AbstractFormatAttribute;
import com.github.fge.jsonschema.processors.data.FullData;
import com.github.fge.msgsimple.bundle.MessageBundle;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.RSQLParserException;

/**
 * Format specifier for {@code elideRSQLFilter} format attribute.
 * <p>
 * This specifier will check if a string instance is a valid RSQL filter.
 * </p>
 */
public class ElideRSQLFilterFormatAttr extends AbstractFormatAttribute {

    public static final String FORMAT_NAME = "elideRSQLFilter";
    public static final String FORMAT_KEY = "elideRSQLFilter.error.format";
    public static final String FORMAT_MSG = "Input value[%s] is not a valid RSQL filter expression. Please visit page "
                    + "https://elide.io/pages/guide/v5/11-graphql.html#operators for samples.";

    public ElideRSQLFilterFormatAttr() {
        super(FORMAT_NAME, NodeType.STRING);
    }

    @Override
    public void validate(final ProcessingReport report, final MessageBundle bundle, final FullData data)
                    throws ProcessingException {
        final String input = data.getInstance().getNode().textValue();

        try {
            new RSQLParser(getDefaultOperatorsWithIsnull()).parse(input);
        } catch (RSQLParserException e) {
            report.error(newMsg(data, bundle, FORMAT_KEY).putArgument("value", input));
        }
    }
}
