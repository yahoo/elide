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

import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Format specifier for {@code elideFieldName} format attribute.
 * <p>
 * This specifier will check if a string instance is a valid Elide Field Name.
 * </p>
 */
public class ElideFieldNameFormatAttr extends AbstractFormatAttribute {
    private static final Pattern FIELD_NAME_FORMAT_REGEX = Pattern.compile("^[a-z][0-9A-Za-z_]*$");
    private static final Set<String> RESERVED_KEYWORDS_FOR_COLUMN_NAME = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    static {
        RESERVED_KEYWORDS_FOR_COLUMN_NAME.add("id");
        RESERVED_KEYWORDS_FOR_COLUMN_NAME.add("sql");
    }

    public static final String FORMAT_NAME = "elideFieldName";
    public static final String NAME_KEY = "elideFieldName.error.name";
    public static final String NAME_MSG =
                    "Field name [%s] is not allowed. Field name cannot be one of " + RESERVED_KEYWORDS_FOR_COLUMN_NAME;
    public static final String FORMAT_KEY = "elideFieldName.error.format";
    public static final String FORMAT_MSG = "Field name [%s] is not allowed. Field name must start with "
                    + "lower case alphabet and can include alaphabets, numbers and '_' only.";

    public ElideFieldNameFormatAttr() {
        super(FORMAT_NAME, NodeType.STRING);
    }

    @Override
    public void validate(final ProcessingReport report, final MessageBundle bundle, final FullData data)
                    throws ProcessingException {
        final String input = data.getInstance().getNode().textValue();

        if (!FIELD_NAME_FORMAT_REGEX.matcher(input).matches()) {
            report.error(newMsg(data, bundle, FORMAT_KEY).putArgument("value", input));
        }

        if (RESERVED_KEYWORDS_FOR_COLUMN_NAME.contains(input)) {
            report.error(newMsg(data, bundle, NAME_KEY).putArgument("value", input));
        }
    }
}
