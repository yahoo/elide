/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.integration;

import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.argument;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.arguments;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.document;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.field;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.selection;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.selections;

import static org.mockito.Mockito.when;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.annotation.SecurityCheck;
import com.yahoo.elide.audit.TestAuditLogger;
import com.yahoo.elide.contrib.dynamicconfighelpers.compile.ConnectionDetails;
import com.yahoo.elide.contrib.dynamicconfighelpers.compile.ElideDynamicEntityCompiler;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.datastores.aggregation.framework.AggregationDataStoreTestHarness;
import com.yahoo.elide.initialization.GraphQLIntegrationTest;
import com.yahoo.elide.resources.JsonApiEndpoint;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.sql.DataSource;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.SecurityContext;

/**
 * Integration tests for Dynamic Configs with Security hjson.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DynamicSecurityHjsonIntegrationTest extends GraphQLIntegrationTest {

    @Mock private static SecurityContext securityContextMock;

    private static final ElideDynamicEntityCompiler COMPILER =
                    AggregationDataStoreIntegrationTest.getCompiler("src/test/resources/configs_with_security_hjson");
    
    private static final class SecurityHjsonIntegrationTestResourceConfig extends ResourceConfig {

        @Inject
        public SecurityHjsonIntegrationTestResourceConfig() {
            register(new AbstractBinder() {
                @Override
                protected void configure() {
                    EntityDictionary dictionary = new EntityDictionary(new HashMap<>());

                    try {
                        dictionary.addSecurityChecks(COMPILER.findAnnotatedClasses(SecurityCheck.class));
                    } catch (ClassNotFoundException e) {
                    }

                    Elide elide = new Elide(new ElideSettingsBuilder(getDataStore())
                                    .withEntityDictionary(dictionary)
                                    .withAuditLogger(new TestAuditLogger())
                                    .build());
                    bind(elide).to(Elide.class).named("elide");
                }
            });
            register(new ContainerRequestFilter() {
                @Override
                public void filter(final ContainerRequestContext requestContext) throws IOException {
                    requestContext.setSecurityContext(securityContextMock);
                }
            });
        }
    }

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
        connectionDetailsMap.putAll(COMPILER.getConnectionDetailsMap());

        return new AggregationDataStoreTestHarness(emf, defaultConnectionDetails, connectionDetailsMap, COMPILER);
    }

    public DynamicSecurityHjsonIntegrationTest() {
        super(SecurityHjsonIntegrationTestResourceConfig.class, JsonApiEndpoint.class.getPackage().getName());
    }

    @Test
    public void testAdminRole() throws Exception {

        when(securityContextMock.isUserInRole("admin")).thenReturn(true);
        when(securityContextMock.isUserInRole("operator")).thenReturn(true);
        when(securityContextMock.isUserInRole("guest user")).thenReturn(true);

        String graphQLRequest = document(
                selection(
                        field(
                                "orderDetails",
                                arguments(
                                        argument("sort", "\"customerRegion\""),
                                        argument("filter", "\"orderMonth=='2020-08'\"")
                                ),
                                selections(
                                        field("orderTotal"),
                                        field("customerRegion"),
                                        field("orderMonth")
                                )
                        )
                )
        ).toQuery();

        String expected = document(
                selection(
                        field(
                                "orderDetails",
                                selections(
                                        field("orderTotal", 61.43F),
                                        field("customerRegion", "NewYork"),
                                        field("orderMonth", "2020-08")
                                ),
                                selections(
                                        field("orderTotal", 113.07F),
                                        field("customerRegion", "Virginia"),
                                        field("orderMonth", "2020-08")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void testOperatorRole() throws Exception {

        when(securityContextMock.isUserInRole("admin")).thenReturn(false);
        when(securityContextMock.isUserInRole("operator")).thenReturn(true);
        when(securityContextMock.isUserInRole("guest user")).thenReturn(true);

        String graphQLRequest = document(
                selection(
                        field(
                                "orderDetails",
                                arguments(
                                        argument("sort", "\"customerRegion\""),
                                        argument("filter", "\"orderMonth=='2020-08'\"")
                                ),
                                selections(
                                        field("customerRegion"),
                                        field("orderMonth")
                                )
                        )
                )
        ).toQuery();

        String expected = document(
                selection(
                        field(
                                "orderDetails",
                                selections(
                                        field("customerRegion", "NewYork"),
                                        field("orderMonth", "2020-08")
                                ),
                                selections(
                                        field("customerRegion", "Virginia"),
                                        field("orderMonth", "2020-08")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void testGuestUserRole() throws Exception {

        when(securityContextMock.isUserInRole("admin")).thenReturn(false);
        when(securityContextMock.isUserInRole("operator")).thenReturn(false);
        when(securityContextMock.isUserInRole("guest user")).thenReturn(true);

        String graphQLRequest = document(
                selection(
                        field(
                                "orderDetails",
                                arguments(
                                        argument("sort", "\"customerRegion\""),
                                        argument("filter", "\"orderMonth=='2020-08'\"")
                                ),
                                selections(
                                        field("customerRegion"),
                                        field("orderMonth")
                                )
                        )
                )
        ).toQuery();

        String expected = "\"Exception while fetching data (/orderDetails/edges[0]/node/customerRegion) : ReadPermission Denied\"";

        runQueryWithExpectedError(graphQLRequest, expected);
    }
}
