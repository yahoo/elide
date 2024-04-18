/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.queryengines.sql;

import com.paiondata.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialect;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.sql.DataSource;

/**
 * Custom class to abstract {@link DataSource} and {@link SQLDialect}.
 */
@Data
@AllArgsConstructor
public class ConnectionDetails {
    private DataSource dataSource;
    private SQLDialect dialect;
}
