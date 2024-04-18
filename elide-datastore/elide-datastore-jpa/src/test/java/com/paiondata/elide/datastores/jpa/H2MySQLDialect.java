/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.jpa;

import org.hibernate.dialect.MySQLDialect;
import org.hibernate.sql.ast.spi.SqlAppender;

/**
 * Dialect for H2 in MySQL mode due to differences.
 */
public class H2MySQLDialect extends MySQLDialect {
    /**
     * Used to generate the correct CASE statement with true or false instead of 0
     * or 1.
     */
    @Override
    public void appendBooleanValueString(SqlAppender appender, boolean bool) {
        appender.appendSql(bool);
    }
}
