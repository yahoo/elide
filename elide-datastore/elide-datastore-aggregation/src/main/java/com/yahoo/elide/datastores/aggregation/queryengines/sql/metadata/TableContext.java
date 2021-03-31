/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.PERIOD;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.applyQuotes;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialect;
import com.github.jknack.handlebars.HandlebarsException;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * TableContext for Handlebars Resolution.
 */
@AllArgsConstructor
@Getter
public class TableContext extends HashMap<String, Object> {

    private final String alias;
    private final SQLDialect dialect;
    private final Map<String, Object> defaultTableArgs;

    public Object get(Object key) {
        // Physical References starts with $
        if (key.toString().lastIndexOf('$') == 0) {
            String resolvedExpr = alias + PERIOD + key.toString().substring(1);
            return applyQuotes(resolvedExpr, dialect);
        }

        verifyKeyExists(key, super.keySet());
        return super.get(key);
    }

    private void verifyKeyExists(Object key, Set<String> keySet) {
        if (!keySet.contains(key)) {
            throw new HandlebarsException(new Throwable("Couldn't find: " + key));
        }
    }
}
