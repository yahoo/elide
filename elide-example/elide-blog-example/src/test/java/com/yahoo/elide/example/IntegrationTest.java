/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.example;

import com.yahoo.elide.standalone.ElideStandalone;
import com.yahoo.elide.standalone.config.ElideStandaloneSettings;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IntegrationTest {
    private ElideStandalone elide;

    protected static final String JDBC_URL = "jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1;MVCC=TRUE;TRACE_LEVEL_FILE=3";
    protected static final String JDBC_USER = "sa";
    protected static final String JDBC_PASSWORD = "";

    @BeforeAll
    public void init() throws Exception {
        elide = new ElideStandalone(new ElideStandaloneSettings() {
            public String getModelPackageName() {

                //This needs to be changed to the package where your models live.
                return "com.yahoo.elide.example.models";
            }

            public String getJsonApiPathSpec() {
                return "/*";
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

        java.sql.Connection connection = getConnection(); //your openConnection logic here

        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));

        Liquibase liquibase = new liquibase.Liquibase(
                "db/changelog/changelog.xml",
                new ClassLoaderResourceAccessor(),
                database);

        liquibase.update("db1");
        connection.commit();
        connection.close();

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
