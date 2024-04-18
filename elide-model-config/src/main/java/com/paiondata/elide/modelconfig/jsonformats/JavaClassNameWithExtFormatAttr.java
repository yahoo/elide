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
 * Format specifier for {@code javaClassNameWithExt} format attribute.
 * <p>
 * This specifier will check if a string instance is a valid JAVA Class Name with {@code .class} extension.
 * </p>
 */
public class JavaClassNameWithExtFormatAttr extends AbstractFormatAttribute {
    private static final Pattern CLASS_NAME_FORMAT_PATTERN =
            Pattern.compile("^(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)+class$");

    public static final String FORMAT_NAME = "javaClassNameWithExt";
    public static final String FORMAT_KEY = "javaClassNameWithExt.error.format";
    public static final String FORMAT_MSG = "Input value[%s] is not a valid Java class name with .class extension.";

    public JavaClassNameWithExtFormatAttr() {
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
