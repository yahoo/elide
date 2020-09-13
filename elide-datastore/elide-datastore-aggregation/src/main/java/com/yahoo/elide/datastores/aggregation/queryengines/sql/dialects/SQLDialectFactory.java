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

    private static final SQLDialect H2_DIALECT = new H2Dialect();
    private static final SQLDialect HIVE_DIALECT = new HiveDialect();
    private static final SQLDialect PRESTO_DIALECT = new PrestoDialect();

    public static SQLDialect getDefaultDialect() {
        return getH2Dialect();
    }

    public static SQLDialect getH2Dialect() {
        return H2_DIALECT;
    }

    public static SQLDialect getHiveDialect() {
        return HIVE_DIALECT;
    }

    public static SQLDialect getPrestoDialect() {
        return PRESTO_DIALECT;
    }

    public static SQLDialect getDialect(String type) {
        if (type.equalsIgnoreCase(H2_DIALECT.getDialectType())) {
            return H2_DIALECT;
        } else if (type.equalsIgnoreCase(HIVE_DIALECT.getDialectType())) {
            return HIVE_DIALECT;
        } else if (type.equalsIgnoreCase(PRESTO_DIALECT.getDialectType())) {
            return PRESTO_DIALECT;
        } else {
            try {
                return (SQLDialect) Class.forName(type).getConstructor().newInstance();
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Unsupported SQL Dialect: " + type, e);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to instantiate SQL Dialect: " + type, e);
            }
        }
    }
}
