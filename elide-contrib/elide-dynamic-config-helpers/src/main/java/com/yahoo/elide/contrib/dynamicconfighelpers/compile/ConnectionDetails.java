/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.compile;

import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.sql.DataSource;

/**
 * Custom class to abstract {@link DataSource} and name of Dialect class.
 */
@AllArgsConstructor
@Getter
public class ConnectionDetails {
    private DataSource dataSource;
    private String dialect;
}
