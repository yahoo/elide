/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.queryengines.sql;

import com.paiondata.elide.modelconfig.DBPasswordExtractor;
import com.paiondata.elide.modelconfig.model.DBConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/**
 * Interface for providing DataSource to use for connection pooling.
 */
public interface DataSourceConfiguration {
    /**
     * Provides DataSource to use for connection pooling.
     * @param dbConfig DB Config POJO.
     * @param dbPasswordExtractor Password Extractor Implementation.
     * @return DataSource Object.
     */
    default DataSource getDataSource(DBConfig dbConfig, DBPasswordExtractor dbPasswordExtractor) {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(dbConfig.getUrl());
        config.setUsername(dbConfig.getUser());
        config.setPassword(dbPasswordExtractor.getDBPassword(dbConfig));
        config.setDriverClassName(dbConfig.getDriver());
        dbConfig.getPropertyMap().forEach((k, v) -> config.addDataSourceProperty(k, v));

        return new HikariDataSource(config);
    }
}
