/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.queryengines.sql.dialects;

import com.paiondata.elide.datastores.aggregation.queryengines.sql.dialects.impl.DruidDialect;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.dialects.impl.H2Dialect;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.dialects.impl.HiveDialect;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.dialects.impl.MySQLDialect;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.dialects.impl.PostgresDialect;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.dialects.impl.PrestoDBDialect;

/**
 * A class with static methods to create an instance of all Dialects.
 */
public class SQLDialectFactory {

    private static final SQLDialect H2_DIALECT = new H2Dialect();
    private static final SQLDialect HIVE_DIALECT = new HiveDialect();
    private static final SQLDialect PRESTODB_DIALECT = new PrestoDBDialect();
    private static final SQLDialect MYSQL_DIALECT = new MySQLDialect();
    private static final SQLDialect POSTGRES_DIALECT = new PostgresDialect();
    private static final SQLDialect DRUID_DIALECT = new DruidDialect();

    public static SQLDialect getDefaultDialect() {
        return getH2Dialect();
    }

    public static SQLDialect getH2Dialect() {
        return H2_DIALECT;
    }

    public static SQLDialect getHiveDialect() {
        return HIVE_DIALECT;
    }

    public static SQLDialect getPrestoDBDialect() {
        return PRESTODB_DIALECT;
    }

    public static SQLDialect getMySQLDialect() {
        return MYSQL_DIALECT;
    }

    public static SQLDialect getPostgresDialect() {
        return POSTGRES_DIALECT;
    }

    public static SQLDialect getDruidDialect() {
        return DRUID_DIALECT;
    }

    public static SQLDialect getDialect(String type) {
        if (type.equalsIgnoreCase(H2_DIALECT.getDialectType())) {
            return H2_DIALECT;
        }
        if (type.equalsIgnoreCase(HIVE_DIALECT.getDialectType())) {
            return HIVE_DIALECT;
        }
        if (type.equalsIgnoreCase(PRESTODB_DIALECT.getDialectType())) {
            return PRESTODB_DIALECT;
        }
        if (type.equalsIgnoreCase(MYSQL_DIALECT.getDialectType())) {
            return MYSQL_DIALECT;
        }
        if (type.equalsIgnoreCase(POSTGRES_DIALECT.getDialectType())) {
            return POSTGRES_DIALECT;
        }
        if (type.equalsIgnoreCase(DRUID_DIALECT.getDialectType())) {
            return DRUID_DIALECT;
        }
        try {
            return Class.forName(type).asSubclass(SQLDialect.class).getConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unsupported SQL Dialect: " + type, e);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to instantiate SQL Dialect: " + type, e);
        }
    }
}
