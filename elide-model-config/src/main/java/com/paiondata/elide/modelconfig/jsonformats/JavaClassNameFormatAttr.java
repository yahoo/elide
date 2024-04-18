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
 * Format specifier for {@code javaClassName} format attribute.
 * <p>
 * This specifier will check if a string instance is a valid JAVA Class Name.
 * </p>
 */
public class JavaClassNameFormatAttr extends AbstractFormatAttribute {
    private static final String ID_PATTERN = "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*";
    public static final Pattern CLASS_NAME_FORMAT_PATTERN = Pattern.compile(ID_PATTERN + "(\\." + ID_PATTERN + ")*");

    public static final String FORMAT_NAME = "javaClassName";
    public static final String FORMAT_KEY = "javaClassName.error.format";
    public static final String FORMAT_MSG = "Input value[%s] is not a valid Java class name.";

    public JavaClassNameFormatAttr() {
        super(FORMAT_NAME, NodeType.STRING);
    }

    @Override
    public void validate(final ProcessingReport report, final MessageBundle bundle, final FullData data)
                    throws ProcessingException {
        final String input = data.getInstance().getNode().textValue();

        if (!CLASS_NAME_FORMAT_PATTERN.matcher(input).matches()) {
            report.error(newMsg(data, bundle, FORMAT_KEY).putArgument("value", input));
        }
    }
}
