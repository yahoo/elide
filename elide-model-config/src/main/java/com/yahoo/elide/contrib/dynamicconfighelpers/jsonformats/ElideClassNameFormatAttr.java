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

public class ElideClassNameFormatAttr extends AbstractFormatAttribute {
    private static final String CLASS_NAME_FORMAT_REGEX = "^([a-zA-Z]+[a-zA-Z0-9_]*\\.)+class$";

    public static final String FORMAT_NAME = "elideClassName";
    public static final String FORMAT_KEY = "elideClassName.error.format";
    public static final String FORMAT_MSG = "Class Name [%s] is not allowed. "
                    + "Class name must follow the format 'X.X.X.X.class'. "
                    + "Here `X` must start with an alphabet and can include alaphabets, numbers and '_' only. "
                    + "Also, number of `X` can vary but must appear atleast once.";

    public ElideClassNameFormatAttr() {
        super(FORMAT_NAME, NodeType.STRING);
    }

    @Override
    public void validate(final ProcessingReport report, final MessageBundle bundle, final FullData data)
                    throws ProcessingException {
        final String input = data.getInstance().getNode().textValue();

        if (!input.matches(CLASS_NAME_FORMAT_REGEX)) {
            report.error(newMsg(data, bundle, FORMAT_KEY).putArgument("value", input));
        }
    }
}
