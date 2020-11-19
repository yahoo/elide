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

/**
 * Format specifier for {@code elideClassName} format attribute.
 * <p>
 * This specifier will check if a string instance is a valid JAVA Class Name with {@code .class} extension.
 * </p>
 */
public class ElideClassNameFormatAttr extends AbstractFormatAttribute {
    private static final String CLASS_NAME_FORMAT_REGEX =
                    "^(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)+class$";

    public static final String FORMAT_NAME = "elideClassName";
    public static final String FORMAT_KEY = "elideClassName.error.format";
    public static final String FORMAT_MSG = "Input value[%s] is not a valid Java class name with .class extension.";

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
