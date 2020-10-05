/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.integration;

import static com.yahoo.elide.core.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.annotation.SecurityCheck;
import com.yahoo.elide.audit.TestAuditLogger;
import com.yahoo.elide.contrib.dynamicconfighelpers.compile.ConnectionDetails;
import com.yahoo.elide.contrib.dynamicconfighelpers.compile.ElideDynamicEntityCompiler;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.datastores.aggregation.framework.AggregationDataStoreTestHarness;
import com.yahoo.elide.initialization.IntegrationTest;
import com.yahoo.elide.resources.SecurityContextUser;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.sql.DataSource;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;

/**
 * Integration tests for Dynamic Configs with Security hjson.
 */
public class AggregationDataStoreDynamicSecurityIT extends IntegrationTest {

    private final ElideDynamicEntityCompiler compiler =
                    AggregationDataStoreIntegrationTest.getCompiler("src/test/resources/configs_with_security_hjson");

    @Override
    protected DataStoreTestHarness createHarness() {

        HikariConfig config = new HikariConfig(File.separator + "jpah2db.properties");
        DataSource defaultDataSource = new HikariDataSource(config);
        String defaultDialect = "h2";
        ConnectionDetails defaultConnectionDetails = new ConnectionDetails(defaultDataSource, defaultDialect);

        Properties prop = new Properties();
        prop.put("javax.persistence.jdbc.driver", config.getDriverClassName());
        prop.put("javax.persistence.jdbc.url", config.getJdbcUrl());
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("aggregationStore", prop);

        Map<String, ConnectionDetails> connectionDetailsMap = new HashMap<>();
        // Add connection details fetched from hjson
        connectionDetailsMap.putAll(compiler.getConnectionDetailsMap());

        return new AggregationDataStoreTestHarness(emf, defaultConnectionDetails, connectionDetailsMap, compiler);
    }

    @Test
    public void testSecurityHjsonWithAggregationModel() throws Exception {

        SecurityContextUser operator = new SecurityContextUser(new SecurityContext() {
            @Override
            public boolean isUserInRole(String role) {
                switch (role) {
                    case "admin":
                        return false;
                    case "operator":
                        return true;
                    case "guest user":
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public boolean isSecure() {
                return false;
            }

            @Override
            public Principal getUserPrincipal() {
                return () -> "1";
            }

            @Override
            public String getAuthenticationScheme() {
                return null;
            }
        });

        SecurityContextUser admin = new SecurityContextUser(new SecurityContext() {
            @Override
            public boolean isUserInRole(String role) {
                switch (role) {
                    case "admin":
                        return true;
                    case "operator":
                        return true;
                    case "guest user":
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public boolean isSecure() {
                return false;
            }

            @Override
            public Principal getUserPrincipal() {
                return () -> "1";
            }

            @Override
            public String getAuthenticationScheme() {
                return null;
            }
        });

        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());
        dictionary.addSecurityChecks(compiler.findAnnotatedClasses(SecurityCheck.class));

        Elide elide = new Elide(new ElideSettingsBuilder(dataStore)
                        .withEntityDictionary(dictionary)
                        .withAuditLogger(new TestAuditLogger()).build());

        String baseUrl = "/";
        ElideResponse response = null;
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<String, String>();
        queryParams.add("sort", "customerRegion,orderMonth");
        queryParams.add("fields[orderDetails]", "orderTotal,customerRegion,orderMonth");
        queryParams.add("filter", "orderMonth>=2020-09");

        // User with "operator" role can not see columns which requires "admin" role
        response = elide.get(baseUrl, "orderDetails", queryParams, operator, NO_VERSION);
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());
        assertEquals("{\"data\":[{\"type\":\"orderDetails\",\"id\":\"0\",\"attributes\":{\"customerRegion\":\"Virginia\""
                        + ",\"orderMonth\":\"2020-09\"}}]}", response.getBody());

        // User with "admin" role can see columns which requires "admin" role
        response = elide.get(baseUrl, "orderDetails", queryParams, admin, NO_VERSION);
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());
        assertEquals("{\"data\":[{\"type\":\"orderDetails\",\"id\":\"0\",\"attributes\":{\"customerRegion\":\"Virginia\""
                        + ",\"orderMonth\":\"2020-09\",\"orderTotal\":260.34}}]}", response.getBody());

    }
}
