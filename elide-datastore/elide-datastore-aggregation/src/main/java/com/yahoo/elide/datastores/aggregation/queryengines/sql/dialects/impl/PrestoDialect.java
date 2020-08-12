/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.impl;

import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.AbstractSqlDialect;

/**
 * Presto SQLDialect
 */
public class PrestoDialect extends AbstractSqlDialect {
    @Override
    public String getDialectType() {
        return "Presto";
    }
}
