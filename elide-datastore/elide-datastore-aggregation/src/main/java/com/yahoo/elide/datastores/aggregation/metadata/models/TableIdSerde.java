/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.utils.coerce.converters.ElideTypeConverter;
import com.yahoo.elide.utils.coerce.converters.Serde;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ElideTypeConverter(type = TableId.class, name = "TableId")
public class TableIdSerde implements Serde<String, TableId> {

    private static final Pattern ID_PATTERN = Pattern.compile("([a-zA-Z0-9_]+)[.]?([a-zA-Z0-9.]*)[-]?([a-zA-Z0-9_]*)");

    @Override
    public TableId deserialize(String val) {
        Matcher matcher = ID_PATTERN.matcher(val);
        if (!matcher.matches()) {
            throw new InvalidValueException(val);
        }

        String name = matcher.group(1);
        String version = matcher.group(2);
        String dbConnectionName = matcher.group(3);

        return new TableId(name, version, dbConnectionName);
    }

    @Override
    public String serialize(TableId val) {
        return val.toString();
    }
}
