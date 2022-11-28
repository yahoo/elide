/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.integration;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.audit.TestAuditLogger;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.security.checks.Check;
import com.yahoo.elide.core.security.checks.prefab.Role;
import com.yahoo.elide.core.utils.DefaultClassScanner;
import com.yahoo.elide.datastores.aggregation.AggregationDataStore;
import com.yahoo.elide.datastores.aggregation.framework.SQLUnitTest;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.ConnectionDetails;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.DataSourceConfiguration;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialect;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.yahoo.elide.initialization.GraphQLIntegrationTest;
import com.yahoo.elide.jsonapi.resources.JsonApiEndpoint;
import com.yahoo.elide.modelconfig.DBPasswordExtractor;
import com.yahoo.elide.modelconfig.model.DBConfig;
import com.yahoo.elide.modelconfig.validator.DynamicConfigValidator;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import example.TestCheckMappings;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.SecurityContext;

/**
 * Integration tests for {@link AggregationDataStore}.
 */
public abstract class AggregationDataStoreIntegrationTest extends GraphQLIntegrationTest {
    @Mock protected static SecurityContext securityContextMock;

    public static DynamicConfigValidator VALIDATOR;

    public static HikariConfig config = new HikariConfig(File.separator + "jpah2db.properties");

    static {
        VALIDATOR = new DynamicConfigValidator(DefaultClassScanner.getInstance(), "src/test/resources/configs");

        try {
            VALIDATOR.readAndValidateConfigs();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected static final class SecurityHjsonIntegrationTestResourceConfig extends ResourceConfig {

        @Inject
        public SecurityHjsonIntegrationTestResourceConfig() {
            register(new AbstractBinder() {
                @Override
                protected void configure() {
                    Map<String, Class<? extends Check>> map = new HashMap<>(TestCheckMappings.MAPPINGS);
                    EntityDictionary dictionary = EntityDictionary.builder().checks(map).build();

                    VALIDATOR.getElideSecurityConfig().getRoles().forEach(role ->
                        dictionary.addRoleCheck(role, new Role.RoleMemberCheck(role))
                    );

                    Elide elide = new Elide(new ElideSettingsBuilder(getDataStore())
                                    .withEntityDictionary(dictionary)
                                    .withAuditLogger(new TestAuditLogger())
                                    .withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", Calendar.getInstance().getTimeZone())
                                    .build());

                    elide.doScans();
                    bind(elide).to(Elide.class).named("elide");
                }
            });
            register((ContainerRequestFilter) requestContext -> requestContext.setSecurityContext(securityContextMock));
        }
    }

    public AggregationDataStoreIntegrationTest() {
        super(SecurityHjsonIntegrationTestResourceConfig.class, JsonApiEndpoint.class.getPackage().getName());
    }

    protected ConnectionDetails createDefaultConnectionDetails() {
        DataSource defaultDataSource = new HikariDataSource(config);
        SQLDialect defaultDialect = SQLDialectFactory.getDefaultDialect();
        return new ConnectionDetails(defaultDataSource, defaultDialect);
    }

    protected EntityManagerFactory createEntityManagerFactory() {
       Properties prop = new Properties();
        prop.put("jakarta.persistence.jdbc.driver", config.getDriverClassName());
        prop.put("jakarta.persistence.jdbc.url", config.getJdbcUrl());
        return Persistence.createEntityManagerFactory("aggregationStore", prop);
    }

    protected Map<String, ConnectionDetails> createConnectionDetailsMap(ConnectionDetails defaultConnectionDetails) {
       Map<String, ConnectionDetails> connectionDetailsMap = new HashMap<>();

        // Add an entry for "mycon" connection which is not from hjson
        connectionDetailsMap.put("mycon", defaultConnectionDetails);
        // Add connection details fetched from hjson
        VALIDATOR.getElideSQLDBConfig().getDbconfigs().forEach(dbConfig ->
            connectionDetailsMap.put(dbConfig.getName(),
                            new ConnectionDetails(getDataSource(dbConfig, getDBPasswordExtractor()),
                                            SQLDialectFactory.getDialect(dbConfig.getDialect())))
        );

        return connectionDetailsMap;
    }

    static DataSource getDataSource(DBConfig dbConfig, DBPasswordExtractor dbPasswordExtractor) {
        return new DataSourceConfiguration() {
        }.getDataSource(dbConfig, dbPasswordExtractor);
    }

    static DBPasswordExtractor getDBPasswordExtractor() {
        return new DBPasswordExtractor() {
            @Override
            public String getDBPassword(DBConfig config) {
                String encrypted = (String) config.getPropertyMap().get("encrypted.password");
                byte[] decrypted = Base64.getDecoder().decode(encrypted.getBytes());
                try {
                    return new String(decrypted, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
    }

    @BeforeAll
    public void beforeAll() {
        SQLUnitTest.init();
    }

    @BeforeEach
    public void setUp() {
        reset(securityContextMock);
        when(securityContextMock.isUserInRole("admin.user")).thenReturn(true);
        when(securityContextMock.isUserInRole("operator")).thenReturn(true);
        when(securityContextMock.isUserInRole("guest user")).thenReturn(true);
    }
}
