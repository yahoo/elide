/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects;

import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.impl.H2Dialect;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.impl.HiveDialect;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.impl.PrestoDialect;

/**
 * A class with static methods to create an instance of all Dialects.
 */
public class SQLDialectFactory {
    public static SQLDialect getDefaultDialect() {
        return new H2Dialect();
    }

    public static SQLDialect getH2Dialect() {
        return new H2Dialect();
    }

    public static SQLDialect getHiveDialect() {
        return new HiveDialect();
    }

    public static SQLDialect getPrestoDialect() {
        return new PrestoDialect();
    }
}
