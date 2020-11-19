/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.compile;

import lombok.Value;

import javax.sql.DataSource;

/**
 * Custom class to abstract {@link DataSource} and name of Dialect class.
 */
@Value
public class ConnectionDetails {
    private DataSource dataSource;
    private String dialect;
}
