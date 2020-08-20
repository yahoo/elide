/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.example;

import com.yahoo.elide.standalone.ElideStandalone;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Base class for running a set of functional Elide tests.  This class
 * sets up an Elide instance with an in-memory H2 database.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IntegrationTest {
    private ElideStandalone elide;

    protected static final String JDBC_URL = "jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1";
    protected static final String JDBC_USER = "sa";
    protected static final String JDBC_PASSWORD = "";

    @BeforeAll
    public void init() throws Exception {
        elide = new ElideStandalone(new CommonElideSettings() {
            @Override
            public int getPort() {
                return 8080;
            }

            @Override
            public Properties getDatabaseProperties() {
                Properties options = new Properties();

                options.put("hibernate.show_sql", "true");
                options.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
                options.put("hibernate.current_session_context_class", "thread");
                options.put("hibernate.jdbc.use_scrollable_resultset", "true");

                options.put("javax.persistence.jdbc.driver", "org.h2.Driver");
                options.put("javax.persistence.jdbc.url", JDBC_URL);
                options.put("javax.persistence.jdbc.user", JDBC_USER);
                options.put("javax.persistence.jdbc.password", JDBC_PASSWORD);

                return options;
            }
        });

        //Run Liquibase Initialization Script
        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(
                new JdbcConnection(getConnection()));

        Liquibase liquibase = new liquibase.Liquibase(
                "db/changelog/changelog.xml",
                new ClassLoaderResourceAccessor(),
                database);

        liquibase.update("db1");

        elide.start(false);
    }

    protected Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
    }

    @AfterAll
    public void shutdown() throws Exception {
        elide.stop();
    }
}
