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
import com.github.fge.jsonschema.processors.data.FullData;
import com.github.fge.msgsimple.bundle.MessageBundle;

public class ElideRSQLFilterFormatAttr extends AbstractFormatAttribute {
    private static final String RSQL_FILTER_FORMAT_REGEX = "^[A-Za-z][0-9A-Za-z_]*(==|!=|>=|>|<|<=|=[a-z]+=)(.*)$";

    public static final String FORMAT_NAME = "elideRSQLFilter";
    public static final String FORMAT_KEY = "elideRSQLFilter.error.format";
    public static final String FORMAT_MSG = "filterTemplate [%s] is not allowed. "
                    + "RSQL filter Template must follow the format 'XoperatorY;XoperatorY;XoperatorY'. "
                    + "Here `X` must start with an alphabet and can include alaphabets, numbers and '_' only. "
                    + "Here `operator` must be one of [==, !=, >=, >, <, <=, =anylowercaseword=]. "
                    + "Here `Y` can be anything and number of `XoperatorY` can vary but must appear atleast once.";

    public ElideRSQLFilterFormatAttr() {
        super(FORMAT_NAME, NodeType.STRING);
    }

    @Override
    public void validate(final ProcessingReport report, final MessageBundle bundle, final FullData data)
                    throws ProcessingException {
        final String input = data.getInstance().getNode().textValue();

        for (String element : input.split(";")) {
            if (!element.matches(RSQL_FILTER_FORMAT_REGEX)) {
                report.error(newMsg(data, bundle, FORMAT_KEY).putArgument("value", input));
            }
        }
    }
}
