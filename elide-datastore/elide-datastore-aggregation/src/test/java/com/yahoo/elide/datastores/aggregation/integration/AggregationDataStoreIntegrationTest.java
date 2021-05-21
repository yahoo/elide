/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.integration;

import static com.yahoo.elide.test.graphql.GraphQLDSL.argument;
import static com.yahoo.elide.test.graphql.GraphQLDSL.arguments;
import static com.yahoo.elide.test.graphql.GraphQLDSL.document;
import static com.yahoo.elide.test.graphql.GraphQLDSL.field;
import static com.yahoo.elide.test.graphql.GraphQLDSL.mutation;
import static com.yahoo.elide.test.graphql.GraphQLDSL.selection;
import static com.yahoo.elide.test.graphql.GraphQLDSL.selections;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.audit.TestAuditLogger;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.HttpStatus;
import com.yahoo.elide.core.security.checks.Check;
import com.yahoo.elide.core.security.checks.prefab.Role;
import com.yahoo.elide.datastores.aggregation.AggregationDataStore;
import com.yahoo.elide.datastores.aggregation.checks.OperatorCheck;
import com.yahoo.elide.datastores.aggregation.checks.VideoGameFilterCheck;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.framework.AggregationDataStoreTestHarness;
import com.yahoo.elide.datastores.aggregation.framework.SQLUnitTest;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.sql.DataSource;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

/**
 * Integration tests for {@link AggregationDataStore}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AggregationDataStoreIntegrationTest extends GraphQLIntegrationTest {

    @Mock private static SecurityContext securityContextMock;

    public static DynamicConfigValidator VALIDATOR;

    static {
        VALIDATOR = new DynamicConfigValidator("src/test/resources/configs");

        try {
            VALIDATOR.readAndValidateConfigs();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final class SecurityHjsonIntegrationTestResourceConfig extends ResourceConfig {

        @Inject
        public SecurityHjsonIntegrationTestResourceConfig() {
            register(new AbstractBinder() {
                @Override
                protected void configure() {
                    Map<String, Class<? extends Check>> map = new HashMap<>(TestCheckMappings.MAPPINGS);
                    map.put(OperatorCheck.OPERTOR_CHECK, OperatorCheck.class);
                    map.put(VideoGameFilterCheck.NAME_FILTER, VideoGameFilterCheck.class);
                    EntityDictionary dictionary = new EntityDictionary(map);

                    VALIDATOR.getElideSecurityConfig().getRoles().forEach(role ->
                        dictionary.addRoleCheck(role, new Role.RoleMemberCheck(role))
                    );

                    Elide elide = new Elide(new ElideSettingsBuilder(getDataStore())
                                    .withEntityDictionary(dictionary)
                                    .withAuditLogger(new TestAuditLogger())
                                    .withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", Calendar.getInstance().getTimeZone())
                                    .build());
                    bind(elide).to(Elide.class).named("elide");
                }
            });
            register((ContainerRequestFilter) requestContext -> requestContext.setSecurityContext(securityContextMock));
        }
    }

    public AggregationDataStoreIntegrationTest() {
        super(SecurityHjsonIntegrationTestResourceConfig.class, JsonApiEndpoint.class.getPackage().getName());
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

    @Override
    protected DataStoreTestHarness createHarness() {

        HikariConfig config = new HikariConfig(File.separator + "jpah2db.properties");
        DataSource defaultDataSource = new HikariDataSource(config);
        SQLDialect defaultDialect = SQLDialectFactory.getDefaultDialect();
        ConnectionDetails defaultConnectionDetails = new ConnectionDetails(defaultDataSource, defaultDialect);

        Properties prop = new Properties();
        prop.put("javax.persistence.jdbc.driver", config.getDriverClassName());
        prop.put("javax.persistence.jdbc.url", config.getJdbcUrl());
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("aggregationStore", prop);

        Map<String, ConnectionDetails> connectionDetailsMap = new HashMap<>();

        // Add an entry for "mycon" connection which is not from hjson
        connectionDetailsMap.put("mycon", defaultConnectionDetails);
        // Add connection details fetched from hjson
        VALIDATOR.getElideSQLDBConfig().getDbconfigs().forEach(dbConfig ->
            connectionDetailsMap.put(dbConfig.getName(),
                            new ConnectionDetails(getDataSource(dbConfig, getDBPasswordExtractor()),
                                            SQLDialectFactory.getDialect(dbConfig.getDialect())))
        );

        return new AggregationDataStoreTestHarness(emf, defaultConnectionDetails, connectionDetailsMap, VALIDATOR);
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

    @Test
    public void testGraphQLSchema() throws IOException {
        String graphQLRequest = "{"
                + "__type(name: \"PlayerStatsWithViewEdge\") {"
                + "   name "
                + "     fields {"
                + "         name "
                + "         type {"
                + "             name"
                + "             fields {"
                + "                 name "
                + "                 type {"
                + "                     name "
                + "                     fields {"
                + "                         name"
                + "                     }"
                + "                 }"
                + "             }"
                + "         }"
                + "     }"
                + "}"
                + "}";

        String expected = loadGraphQLResponse("testGraphQLSchema.json");

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void basicAggregationTest() throws Exception {
        String graphQLRequest = document(
                selection(
                        field(
                                "playerStats",
                                arguments(
                                        argument("sort", "\"highScore\"")
                                ),
                                selections(
                                        field("highScore"),
                                        field("overallRating"),
                                        field("countryIsoCode"),
                                        field("playerRank")
                                )
                        )
                )
        ).toQuery();

        String expected = document(
                selections(
                        field(
                                "playerStats",
                                selections(
                                        field("highScore", 1000),
                                        field("overallRating", "Good"),
                                        field("countryIsoCode", "HKG"),
                                        field("playerRank", 3)
                                ),
                                selections(
                                        field("highScore", 1234),
                                        field("overallRating", "Good"),
                                        field("countryIsoCode", "USA"),
                                        field("playerRank", 1)
                                ),
                                selections(
                                        field("highScore", 2412),
                                        field("overallRating", "Great"),
                                        field("countryIsoCode", "USA"),
                                        field("playerRank", 2)
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void metricFormulaTest() throws Exception {
        String graphQLRequest = document(
                selection(
                        field(
                                "videoGame",
                                arguments(
                                        argument("sort", "\"timeSpentPerSession\"")
                                ),
                                selections(
                                        field("timeSpent"),
                                        field("sessions"),
                                        field("timeSpentPerSession"),
                                        field("playerName")
                                )
                        )
                )
        ).toQuery();

        String expected = document(
                selections(
                        field(
                                "videoGame",
                                selections(
                                        field("timeSpent", 720),
                                        field("sessions", 60),
                                        field("timeSpentPerSession", 12.0),
                                        field("playerName", "Jon Doe")
                                ),
                                selections(
                                        field("timeSpent", 350),
                                        field("sessions", 25),
                                        field("timeSpentPerSession", 14.0),
                                        field("playerName", "Jane Doe")
                                ),
                                selections(
                                        field("timeSpent", 300),
                                        field("sessions", 10),
                                        field("timeSpentPerSession", 30.0),
                                        field("playerName", "Han")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);

        //When admin = false

        when(securityContextMock.isUserInRole("admin.user")).thenReturn(false);

        expected = document(
                selections(
                        field(
                                "videoGame",
                                selections(
                                        field("timeSpent", 720),
                                        field("sessions", 60),
                                        field("timeSpentPerSession", 12.0),
                                        field("playerName", "Jon Doe")
                                ),
                                selections(
                                        field("timeSpent", 350),
                                        field("sessions", 25),
                                        field("timeSpentPerSession", 14.0),
                                        field("playerName", "Jane Doe")
                                )
                        )
                )
        ).toResponse();
        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    /**
     * Test sql expression in where, sorting, group by and projection.
     * @throws Exception exception
     */
    @Test
    public void dimensionFormulaTest() throws Exception {
        String graphQLRequest = document(
                selection(
                        field(
                                "playerStats",
                                arguments(
                                        argument("sort", "\"playerLevel\""),
                                        argument("filter", "\"playerLevel>\\\"0\\\"\"")
                                ),
                                selections(
                                        field("highScore"),
                                        field("playerLevel")
                                )
                        )
                )
        ).toQuery();

        String expected = document(
                selections(
                        field(
                                "playerStats",
                                selections(
                                        field("highScore", 1234),
                                        field("playerLevel", 1)
                                ),
                                selections(
                                        field("highScore", 2412),
                                        field("playerLevel", 2)
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void noMetricQueryTest() throws Exception {
        String graphQLRequest = document(
                selection(
                        field(
                                "playerStatsWithView",
                                arguments(
                                        argument("sort", "\"countryViewViewIsoCode\"")
                                ),
                                selections(
                                        field("countryViewViewIsoCode")
                                )
                        )
                )
        ).toQuery();

        String expected = document(
                selections(
                        field(
                                "playerStatsWithView",
                                selections(
                                        field("countryViewViewIsoCode", "HKG")
                                ),
                                selections(
                                        field("countryViewViewIsoCode", "USA")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void whereFilterTest() throws Exception {
        String graphQLRequest = document(
                selection(
                        field(
                                "playerStats",
                                arguments(
                                        argument("filter", "\"overallRating==\\\"Good\\\"\"")
                                ),
                                selections(
                                        field("highScore"),
                                        field("overallRating")
                                )
                        )
                )
        ).toQuery();

        String expected = document(
                selections(
                        field(
                                "playerStats",
                                selections(
                                        field("highScore", 1234),
                                        field("overallRating", "Good")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void havingFilterTest() throws Exception {
        String graphQLRequest = document(
                selection(
                        field(
                                "playerStats",
                                arguments(
                                        argument("filter", "\"lowScore<\\\"45\\\"\"")
                                ),
                                selections(
                                        field("lowScore"),
                                        field("overallRating"),
                                        field("playerName")
                                )
                        )
                )
        ).toQuery();

        String expected = document(
                selections(
                        field(
                                "playerStats",
                                selections(
                                        field("lowScore", 35),
                                        field("overallRating", "Good"),
                                        field("playerName", "Jon Doe")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    /**
     * Test the case that a where clause is promoted into having clause.
     * @throws Exception exception
     */
    @Test
    public void wherePromotionTest() throws Exception {
        String graphQLRequest = document(
                selection(
                        field(
                                "playerStats",
                                arguments(
                                        argument("filter", "\"overallRating==\\\"Good\\\",lowScore<\\\"45\\\"\""),
                                        argument("sort", "\"lowScore\"")
                                ),
                                selections(
                                        field("lowScore"),
                                        field("overallRating"),
                                        field("playerName")
                                )
                        )
                )
        ).toQuery();

        String expected = document(
                selections(
                        field(
                                "playerStats",
                                selections(
                                        field("lowScore", 35),
                                        field("overallRating", "Good"),
                                        field("playerName", "Jon Doe")
                                ),
                                selections(
                                        field("lowScore", 72),
                                        field("overallRating", "Good"),
                                        field("playerName", "Han")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    /**
     * Test the case that a where clause, which requires dimension join, is promoted into having clause.
     * @throws Exception exception
     */
    @Test
    public void havingClauseJoinTest() throws Exception {
        String graphQLRequest = document(
                selection(
                        field(
                                "playerStats",
                                arguments(
                                        argument("filter", "\"countryIsoCode==\\\"USA\\\",lowScore<\\\"45\\\"\""),
                                        argument("sort", "\"lowScore\"")
                                ),
                                selections(
                                        field("lowScore"),
                                        field("countryIsoCode"),
                                        field("playerName")
                                )
                        )
                )
        ).toQuery();

        String expected = document(
                selections(
                        field(
                                "playerStats",
                                selections(
                                        field("lowScore", 35),
                                        field("countryIsoCode", "USA"),
                                        field("playerName", "Jon Doe")
                                ),
                                selections(
                                        field("lowScore", 241),
                                        field("countryIsoCode", "USA"),
                                        field("playerName", "Jane Doe")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    /**
     * Test invalid where promotion on a dimension field that is not grouped.
     * @throws Exception exception
     */
    @Test
    public void ungroupedHavingDimensionTest() throws Exception {
        String graphQLRequest = document(
                selection(
                        field(
                                "playerStats",
                                arguments(
                                        argument("filter", "\"countryIsoCode==\\\"USA\\\",lowScore<\\\"45\\\"\"")
                                ),
                                selections(
                                        field("lowScore")
                                )
                        )
                )
        ).toQuery();

        String errorMessage = "Exception while fetching data (/playerStats) : Invalid operation: "
                + "Post aggregation filtering on &#39;countryIsoCode&#39; requires the field to be projected in the response";

        runQueryWithExpectedError(graphQLRequest, errorMessage);
    }

    /**
     * Test invalid having clause on a metric field that is not aggregated.
     * @throws Exception exception
     */
    @Test
    public void nonAggregatedHavingMetricTest() throws Exception {
        String graphQLRequest = document(
                selection(
                        field(
                                "playerStats",
                                arguments(
                                        argument("filter", "\"highScore>\\\"0\\\"\"")
                                ),
                                selections(
                                        field("lowScore")
                                )
                        )
                )
        ).toQuery();

        String expected = document(
                selections(
                        field(
                                "playerStats",
                                selections(
                                        field("lowScore", 35)
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    /**
     * Test invalid where promotion on a different class than the queried class.
     * @throws Exception exception
     */
    @Test
    public void invalidHavingClauseClassTest() throws Exception {
        String graphQLRequest = document(
                selection(
                        field(
                                "playerStats",
                                arguments(
                                        argument("filter", "\"country.isoCode==\\\"USA\\\",lowScore<\\\"45\\\"\"")
                                ),
                                selections(
                                        field("lowScore")
                                )
                        )
                )
        ).toQuery();

        String errorMessage = "Exception while fetching data (/playerStats) : Invalid operation: "
                + "Relationship traversal not supported for analytic queries.";

        runQueryWithExpectedError(graphQLRequest, errorMessage);
    }

    @Test
    public void dimensionSortingTest() throws Exception {
        String graphQLRequest = document(
                selection(
                        field(
                                "playerStats",
                                arguments(
                                        argument("sort", "\"overallRating\"")
                                ),
                                selections(
                                        field("lowScore"),
                                        field("overallRating")
                                )
                        )
                )
        ).toQuery();

        String expected = document(
                selections(
                        field(
                                "playerStats",
                                selections(
                                        field("lowScore", 35),
                                        field("overallRating", "Good")
                                ),
                                selections(
                                        field("lowScore", 241),
                                        field("overallRating", "Great")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void metricSortingTest() throws Exception {
        String graphQLRequest = document(
                selection(
                        field(
                                "playerStats",
                                arguments(
                                        argument("sort", "\"-highScore\"")
                                ),
                                selections(
                                        field("highScore"),
                                        field("countryIsoCode")
                                )
                        )
                )
        ).toQuery();

        String expected = document(
                selections(
                        field(
                                "playerStats",
                                selections(
                                        field("highScore", 2412),
                                        field("countryIsoCode", "USA")
                                ),
                                selections(
                                        field("highScore", 1000),
                                        field("countryIsoCode", "HKG")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void multipleColumnsSortingTest() throws Exception {
        String graphQLRequest = document(
                selection(
                        field(
                                "playerStats",
                                arguments(
                                        argument("sort", "\"overallRating,playerName\"")
                                ),
                                selections(
                                        field("lowScore"),
                                        field("overallRating"),
                                        field("playerName")
                                )
                        )
                )
        ).toQuery();

        String expected = document(
                selections(
                        field(
                                "playerStats",
                                selections(
                                        field("lowScore", 72),
                                        field("overallRating", "Good"),
                                        field("playerName", "Han")
                                ),
                                selections(
                                        field("lowScore", 35),
                                        field("overallRating", "Good"),
                                        field("playerName", "Jon Doe")
                                ),
                                selections(
                                        field("lowScore", 241),
                                        field("overallRating", "Great"),
                                        field("playerName", "Jane Doe")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void idSortingTest() throws Exception {
        String graphQLRequest = document(
                selection(
                        field(
                                "playerStats",
                                arguments(
                                        argument("sort", "\"id\"")
                                ),
                                selections(
                                        field("lowScore"),
                                        field("id")
                                )
                        )
                )
        ).toQuery();

        String expected = "Exception while fetching data (/playerStats) : Invalid operation: Sorting on id field is not permitted";

        runQueryWithExpectedError(graphQLRequest, expected);
    }

    @Test
    public void nestedDimensionNotInQuerySortingTest() throws Exception {
        String graphQLRequest = document(
                selection(
                        field(
                                "playerStats",
                                arguments(
                                        argument("sort", "\"-countryIsoCode,lowScore\"")
                                ),
                                selections(
                                        field("lowScore")
                                )
                        )
                )
        ).toQuery();

        String expected = "Exception while fetching data (/playerStats) : Invalid operation: Can not sort on countryIsoCode as it is not present in query";

        runQueryWithExpectedError(graphQLRequest, expected);
    }

    @Test
    public void sortingOnMetricNotInQueryTest() throws Exception {
        String graphQLRequest = document(
                selection(
                        field(
                                "playerStats",
                                arguments(
                                        argument("sort", "\"highScore\"")
                                ),
                                selections(
                                        field("lowScore"),
                                        field("countryIsoCode")
                                )
                        )
                )
        ).toQuery();

        String expected = "Exception while fetching data (/playerStats) : Invalid operation: Can not sort on highScore as it is not present in query";

        runQueryWithExpectedError(graphQLRequest, expected);
    }

    @Test
    public void basicViewAggregationTest() throws Exception {
        String graphQLRequest = document(
                selection(
                        field(
                                "playerStatsWithView",
                                arguments(
                                        argument("sort", "\"highScore\"")
                                ),
                                selections(
                                        field("highScore"),
                                        field("countryViewIsoCode")
                                )
                        )
                )
        ).toQuery();

        String expected = document(
                selections(
                        field(
                                "playerStatsWithView",
                                selections(
                                        field("highScore", 1000),
                                        field("countryViewIsoCode", "HKG")
                                ),
                                selections(
                                        field("highScore", 2412),
                                        field("countryViewIsoCode", "USA")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void multiTimeDimensionTest() throws IOException {
        String graphQLRequest = document(
                selection(
                        field(
                                "playerStats",
                                selections(
                                        field("recordedDate"),
                                        field("updatedDate")
                                )
                        )
                )
        ).toQuery();

        String expected = document(
                selections(
                        field(
                                "playerStats",
                                selections(
                                        field("recordedDate", "2019-07-13"),
                                        field("updatedDate", "2020-01-12")
                                ),
                                selections(
                                        field("recordedDate", "2019-07-12"),
                                        field("updatedDate", "2019-10-12")
                                ),
                                selections(
                                        field("recordedDate", "2019-07-11"),
                                        field("updatedDate", "2020-07-12")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void testGraphqlQueryDynamicModelById() throws IOException {
        String graphQLRequest = document(
                selection(
                        field(
                                "SalesNamespace_orderDetails",
                                selections(
                                        field("id"),
                                        field("orderTotal")
                                )
                        )
                )
        ).toQuery();

        String expected = document(
                selections(
                        field(
                                "SalesNamespace_orderDetails",
                                selections(
                                        field("id", "0"),
                                        field("orderTotal", 434.84)
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void jsonApiAggregationTest() {
        given()
                .accept("application/vnd.api+json")
                .get("/playerStats")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.id", hasItems("0", "1", "2"))
                .body("data.attributes.highScore", hasItems(1000, 1234, 2412))
                .body("data.attributes.countryIsoCode", hasItems("USA", "HKG"));
    }

    /**
     * Below tests demonstrate using the aggregation store from dynamic configuration.
     */
    @Test
    public void testDynamicAggregationModel() {
        String getPath = "/SalesNamespace_orderDetails?sort=customerRegion,orderTime&page[totals]&"
                        + "fields[SalesNamespace_orderDetails]=orderTotal,customerRegion,orderTime&filter=orderTime>=2020-08";
        given()
            .when()
            .get(getPath)
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body("data", hasSize(3))
            .body("data.id", hasItems("0", "1", "2"))
            .body("data.attributes", hasItems(
                            allOf(hasEntry("customerRegion", "NewYork"), hasEntry("orderTime", "2020-08")),
                            allOf(hasEntry("customerRegion", "Virginia"), hasEntry("orderTime", "2020-08")),
                            allOf(hasEntry("customerRegion", "Virginia"), hasEntry("orderTime", "2020-09"))))
            .body("data.attributes.orderTotal", hasItems(61.43F, 113.07F, 260.34F))
            .body("meta.page.number", equalTo(1))
            .body("meta.page.totalRecords", equalTo(3))
            .body("meta.page.totalPages", equalTo(1))
            .body("meta.page.limit", equalTo(500));
    }

    @Test
    public void testInvalidSparseFields() {
        String expectedError = "Invalid value: SalesNamespace_orderDetails does not contain the fields: [orderValue, customerState]";
        String getPath = "/SalesNamespace_orderDetails?fields[SalesNamespace_orderDetails]=orderValue,customerState,orderTime";
        given()
            .when()
            .get(getPath)
            .then()
            .statusCode(HttpStatus.SC_BAD_REQUEST)
            .body("errors.detail", hasItems(expectedError));
    }

    @Test
    public void missingClientFilterTest() {
        String expectedError = "Querying SalesNamespace_deliveryDetails requires a mandatory filter:"
                + " month&gt;={{start}};month&lt;{{end}}";
        when()
        .get("/SalesNamespace_deliveryDetails/")
        .then()
        .body("errors.detail", hasItems(expectedError))
        .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void incompleteClientFilterTest() {
        String expectedError = "Querying SalesNamespace_deliveryDetails requires a mandatory filter:"
                + " month&gt;={{start}};month&lt;{{end}}";
        when()
        .get("/SalesNamespace_deliveryDetails?filter=month>=2020-08")
        .then()
        .body("errors.detail", hasItems(expectedError))
        .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void completeClientFilterTest() {
        when()
        .get("/SalesNamespace_deliveryDetails?filter=month>=2020-08;month<2020-09")
        .then()
        .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void testGraphQLDynamicAggregationModel() throws Exception {
        String graphQLRequest = document(
                selection(
                        field(
                                "SalesNamespace_orderDetails",
                                arguments(
                                        argument("sort", "\"customerRegion\""),
                                        argument("filter", "\"orderTime=='2020-08'\"")
                                ),
                                selections(
                                        field("orderTotal"),
                                        field("customerRegion"),
                                        field("customerRegionRegion"),
                                        field("orderTime", arguments(
                                                argument("grain", TimeGrain.MONTH)
                                        ))
                                )
                        )
                )
        ).toQuery();

        String expected = document(
                selections(
                        field(
                                "SalesNamespace_orderDetails",
                                selections(
                                        field("orderTotal", 61.43F),
                                        field("customerRegion", "NewYork"),
                                        field("customerRegionRegion", "NewYork"),
                                        field("orderTime", "2020-08")
                                ),
                                selections(
                                        field("orderTotal", 113.07F),
                                        field("customerRegion", "Virginia"),
                                        field("customerRegionRegion", "Virginia"),
                                        field("orderTime", "2020-08")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    /**
     * Tests for below type of column references.
     *
     * a) Physical Column Reference in same table.
     * b) Logical Column Reference in same table, which references Physical column in same table.
     * c) Logical Column Reference in same table, which references another Logical column in same table, which
     *  references Physical column in same table.
     * d) Physical Column Reference in referred table.
     * e) Logical Column Reference in referred table, which references Physical column in referred table.
     * f) Logical Column Reference in referred table, which references another Logical column in referred table, which
     *  references another Logical column in referred table, which references Physical column in referred table.
     * g) Logical Column Reference in same table, which references Physical column in referred table.
     * h) Logical Column Reference in same table, which references another Logical Column in referred table, which
     *  references another Logical column in referred table, which references another Logical column in referred table,
     *  which references Physical column in referred table
     *
     * @throws Exception
     */
    @Test
    public void testGraphQLDynamicAggregationModelAllFields() throws Exception {
        String graphQLRequest = document(
                selection(
                        field(
                                "SalesNamespace_orderDetails",
                                arguments(
                                        argument("sort", "\"courierName,deliveryDate,orderTotal\""),
                                        argument("filter", "\"deliveryDate>='2020-09-01',orderTotal>50\"")
                                ),
                                selections(
                                        field("courierName"),
                                        field("deliveryTime"),
                                        field("deliveryHour"),
                                        field("deliveryDate"),
                                        field("deliveryMonth"),
                                        field("deliveryYear"),
                                        field("deliveryDefault"),
                                        field("orderTime", "bySecond", arguments(
                                                argument("grain", TimeGrain.SECOND)
                                        )),
                                        field("orderTime", "byDay", arguments(
                                                argument("grain", TimeGrain.DAY)
                                        )),
                                        field("orderTime", "byMonth", arguments(
                                                argument("grain", TimeGrain.MONTH)
                                        )),
                                        field("customerRegion"),
                                        field("customerRegionRegion"),
                                        field("orderTotal"),
                                        field("zipCode"),
                                        field("orderId")
                                )
                        )
                )
        ).toQuery();

        String expected = document(
                selections(
                        field(
                                "SalesNamespace_orderDetails",
                                selections(
                                        field("courierName", "FEDEX"),
                                        field("deliveryTime", "2020-09-11T16:30:11"),
                                        field("deliveryHour", "2020-09-11T16"),
                                        field("deliveryDate", "2020-09-11"),
                                        field("deliveryMonth", "2020-09"),
                                        field("deliveryYear", "2020"),
                                        field("bySecond", "2020-09-08T16:30:11"),
                                        field("deliveryDefault", "2020-09-11"),
                                        field("byDay", "2020-09-08"),
                                        field("byMonth", "2020-09"),
                                        field("customerRegion", "Virginia"),
                                        field("customerRegionRegion", "Virginia"),
                                        field("orderTotal", 84.11F),
                                        field("zipCode", 20166),
                                        field("orderId", "order-1b")
                                ),
                                selections(
                                        field("courierName", "FEDEX"),
                                        field("deliveryTime", "2020-09-11T16:30:11"),
                                        field("deliveryHour", "2020-09-11T16"),
                                        field("deliveryDate", "2020-09-11"),
                                        field("deliveryMonth", "2020-09"),
                                        field("deliveryYear", "2020"),
                                        field("bySecond", "2020-09-08T16:30:11"),
                                        field("deliveryDefault", "2020-09-11"),
                                        field("byDay", "2020-09-08"),
                                        field("byMonth", "2020-09"),
                                        field("customerRegion", "Virginia"),
                                        field("customerRegionRegion", "Virginia"),
                                        field("orderTotal", 97.36F),
                                        field("zipCode", 20166),
                                        field("orderId", "order-1c")
                                ),
                                selections(
                                        field("courierName", "UPS"),
                                        field("deliveryTime", "2020-09-05T16:30:11"),
                                        field("deliveryHour", "2020-09-05T16"),
                                        field("deliveryDate", "2020-09-05"),
                                        field("deliveryMonth", "2020-09"),
                                        field("deliveryYear", "2020"),
                                        field("bySecond", "2020-08-30T16:30:11"),
                                        field("deliveryDefault", "2020-09-05"),
                                        field("byDay", "2020-08-30"),
                                        field("byMonth", "2020-08"),
                                        field("customerRegion", "Virginia"),
                                        field("customerRegionRegion", "Virginia"),
                                        field("orderTotal", 103.72F),
                                        field("zipCode", 20166),
                                        field("orderId", "order-1a")
                                ),
                                selections(
                                        field("courierName", "UPS"),
                                        field("deliveryTime", "2020-09-13T16:30:11"),
                                        field("deliveryHour", "2020-09-13T16"),
                                        field("deliveryDate", "2020-09-13"),
                                        field("deliveryMonth", "2020-09"),
                                        field("deliveryYear", "2020"),
                                        field("bySecond", "2020-09-09T16:30:11"),
                                        field("deliveryDefault", "2020-09-13"),
                                        field("byDay", "2020-09-09"),
                                        field("byMonth", "2020-09"),
                                        field("customerRegion", "Virginia"),
                                        field("customerRegionRegion", "Virginia"),
                                        field("orderTotal", 78.87F),
                                        field("zipCode", 20170),
                                        field("orderId", "order-3b")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void testGraphQLDynamicAggregationModelDateTime() throws Exception {
        String graphQLRequest = document(
                selection(
                        field(
                                "SalesNamespace_orderDetails",
                                arguments(
                                        argument("sort", "\"customerRegion\""),
                                        argument("filter", "\"bySecond=='2020-09-08T16:30:11'\"")
                                ),
                                selections(
                                        field("orderTotal"),
                                        field("customerRegion"),
                                        field("orderTime", "byMonth", arguments(
                                                argument("grain", TimeGrain.MONTH)
                                        )),
                                        field("orderTime", "bySecond", arguments(
                                                argument("grain", TimeGrain.SECOND)
                                        ))
                                )
                        )
                )
        ).toQuery();

        String expected = document(
                selections(
                        field(
                                "SalesNamespace_orderDetails",
                                selections(
                                        field("orderTotal", 181.47F),
                                        field("customerRegion", "Virginia"),
                                        field("byMonth", "2020-09"),
                                        field("bySecond", "2020-09-08T16:30:11")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void testTimeDimMismatchArgs() throws Exception {

        String graphQLRequest = document(
                selection(
                        field(
                                "SalesNamespace_orderDetails",
                                arguments(
                                        argument("sort", "\"customerRegion\""),
                                        argument("filter", "\"orderTime[grain:DAY]=='2020-08',orderTotal>50\"")
                                ),
                                selections(
                                        field("orderTotal"),
                                        field("customerRegion"),
                                        field("orderTime", arguments(
                                                argument("grain", TimeGrain.MONTH) // Does not match grain argument in filter
                                        ))
                                )
                        )
                )
        ).toQuery();

        String expected = "Exception while fetching data (/SalesNamespace_orderDetails) : Invalid operation: Post aggregation filtering on &#39;orderTime&#39; requires the field to be projected in the response with matching arguments";

        runQueryWithExpectedError(graphQLRequest, expected);
    }

    @Test
    public void testTimeDimMismatchArgsWithDefaultSelect() throws Exception {

        String graphQLRequest = document(
                selection(
                        field(
                                "SalesNamespace_orderDetails",
                                arguments(
                                        argument("sort", "\"customerRegion\""),
                                        argument("filter", "\"orderTime[grain:DAY]=='2020-08',orderTotal>50\"")
                                ),
                                selections(
                                        field("orderTotal"),
                                        field("customerRegion"),
                                        field("orderTime") //Default Grain for OrderTime is Month.
                                )
                        )
                )
        ).toQuery();

        String expected = "Exception while fetching data (/SalesNamespace_orderDetails) : Invalid operation: Post aggregation filtering on &#39;orderTime&#39; requires the field to be projected in the response with matching arguments";


        runQueryWithExpectedError(graphQLRequest, expected);
    }

    @Test
    public void testTimeDimMismatchArgsWithDefaultFilter() throws Exception {

        String graphQLRequest = document(
                selection(
                        field(
                                "SalesNamespace_orderDetails",
                                arguments(
                                        argument("sort", "\"orderTime\""),
                                        argument("filter", "\"orderTime=='2020-08-01',orderTotal>50\"") //No Grain Arg passed, so works based on Alias's argument in Selection.
                                ),
                                selections(
                                        field("orderTotal"),
                                        field("customerRegion"),
                                        field("orderTime", arguments(
                                                argument("grain", TimeGrain.DAY)
                                        ))
                                )
                        )
                )
        ).toQuery();

        String expected = document(
                selections(
                        field(
                                "SalesNamespace_orderDetails",
                                selections(
                                        field("orderTotal", 103.72F),
                                        field("customerRegion", "Virginia"),
                                        field("orderTime", "2020-08-30")
                                ),
                                selections(
                                        field("orderTotal", 181.47F),
                                        field("customerRegion", "Virginia"),
                                        field("orderTime", "2020-09-08")
                                ),
                                selections(
                                        field("orderTotal", 78.87F),
                                        field("customerRegion", "Virginia"),
                                        field("orderTime", "2020-09-09")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void testAdminRole() throws Exception {

        String graphQLRequest = document(
                selection(
                        field(
                                "SalesNamespace_orderDetails",
                                arguments(
                                        argument("sort", "\"customerRegion\""),
                                        argument("filter", "\"orderTime=='2020-08'\"")
                                ),
                                selections(
                                        field("orderTotal"),
                                        field("customerRegion"),
                                        field("orderTime", arguments(
                                                argument("grain", TimeGrain.MONTH)
                                        ))
                                )
                        )
                )
        ).toQuery();

        String expected = document(
                selection(
                        field(
                                "SalesNamespace_orderDetails",
                                selections(
                                        field("orderTotal", 61.43F),
                                        field("customerRegion", "NewYork"),
                                        field("orderTime", "2020-08")
                                ),
                                selections(
                                        field("orderTotal", 113.07F),
                                        field("customerRegion", "Virginia"),
                                        field("orderTime", "2020-08")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void testOperatorRole() throws Exception {

        when(securityContextMock.isUserInRole("admin")).thenReturn(false);

        String graphQLRequest = document(
                selection(
                        field(
                                "SalesNamespace_orderDetails",
                                arguments(
                                        argument("sort", "\"customerRegion\""),
                                        argument("filter", "\"orderTime=='2020-08'\"")
                                ),
                                selections(
                                        field("customerRegion"),
                                        field("orderTime", arguments(
                                                argument("grain", TimeGrain.MONTH)
                                        ))
                                )
                        )
                )
        ).toQuery();

        String expected = document(
                selection(
                        field(
                                "SalesNamespace_orderDetails",
                                selections(
                                        field("customerRegion", "NewYork"),
                                        field("orderTime", "2020-08")
                                ),
                                selections(
                                        field("customerRegion", "Virginia"),
                                        field("orderTime", "2020-08")
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

        String graphQLRequest = document(
                selection(
                        field(
                                "SalesNamespace_orderDetails",
                                arguments(
                                        argument("sort", "\"customerRegion\""),
                                        argument("filter", "\"orderTime=='2020-08'\"")
                                ),
                                selections(
                                        field("customerRegion"),
                                        field("orderTime", arguments(
                                                argument("grain", TimeGrain.MONTH)
                                        ))
                                )
                        )
                )
        ).toQuery();

        String expected = "Exception while fetching data (/SalesNamespace_orderDetails/edges[0]/node/customerRegion) : ReadPermission Denied";

        runQueryWithExpectedError(graphQLRequest, expected);
    }

    @Test
    public void testTimeDimensionAliases() throws Exception {

        String graphQLRequest = document(
                selection(
                        field(
                                "playerStats",
                                arguments(
                                        argument("filter", "\"byDay>='2019-07-12'\""),
                                        argument("sort", "\"byDay\"")
                                ),
                                selections(
                                        field("highScore"),
                                        field("recordedDate", "byDay", arguments(
                                                argument("grain", TimeGrain.DAY)
                                        )),
                                        field("recordedDate", "byMonth", arguments(
                                                argument("grain", TimeGrain.MONTH)
                                        )),
                                        field("recordedDate", "byQuarter", arguments(
                                                argument("grain", TimeGrain.QUARTER)
                                        ))
                                )
                        )
                )
        ).toQuery();

        String expected = document(
                selections(
                        field(
                                "playerStats",
                                selections(
                                        field("highScore", 1234),
                                        field("byDay", "2019-07-12"),
                                        field("byMonth", "2019-07"),
                                        field("byQuarter", "2019-07")
                                ),
                                selections(
                                        field("highScore", 1000),
                                        field("byDay", "2019-07-13"),
                                        field("byMonth", "2019-07"),
                                        field("byQuarter", "2019-07")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void testTimeDimensionArgumentsInFilter() throws Exception {

        String graphQLRequest = document(
                selection(
                        field(
                                "SalesNamespace_orderDetails",
                                arguments(
                                        argument("sort", "\"customerRegion\""),
                                        argument("filter", "\"orderTime[grain:day]=='2020-09-08'\"")
                                ),
                                selections(
                                        field("customerRegion"),
                                        field("orderTotal"),
                                        field("orderTime", arguments(
                                                   argument("grain", TimeGrain.MONTH)
                                        ))
                                )
                        )
                )
        ).toQuery();

        String expected = document(
                selection(
                        field(
                                "SalesNamespace_orderDetails",
                                selections(
                                        field("customerRegion", "Virginia"),
                                        field("orderTotal", 181.47F),
                                        field("orderTime", "2020-09")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void testSchemaIntrospection() throws Exception {
        String graphQLRequest = "{"
                + "__schema {"
                + "   mutationType {"
                + "     name "
                + "     fields {"
                + "       name "
                + "       args {"
                + "          name"
                + "          defaultValue"
                + "       }"
                + "     }"
                + "   }"
                + "}"
                + "}";

        String query = toJsonQuery(graphQLRequest, new HashMap<>());

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(query)
            .post("/graphQL")
            .then()
            .statusCode(HttpStatus.SC_OK)
            // Verify that the SalesNamespace_orderDetails Model has an argument "denominator".
            .body("data.__schema.mutationType.fields.find { it.name == 'SalesNamespace_orderDetails' }.args.name[7] ", equalTo("denominator"));

        graphQLRequest = "{"
                + "__type(name: \"SalesNamespace_orderDetails\") {"
                + "   name"
                + "   fields {"
                + "     name "
                + "     args {"
                + "        name"
                + "        defaultValue"
                + "     }"
                + "   }"
                + "}"
                + "}";

        query = toJsonQuery(graphQLRequest, new HashMap<>());

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(query)
            .post("/graphQL")
            .then()
            .statusCode(HttpStatus.SC_OK)
            // Verify that the orderTotal attribute has an argument "precision".
            .body("data.__type.fields.find { it.name == 'orderTotal' }.args.name[0]", equalTo("precision"));
    }

    @Test
    public void testDelete() throws IOException {
        String graphQLRequest = mutation(
                selection(
                        field(
                                "playerStats",
                                arguments(
                                        argument("op", "DELETE"),
                                        argument("ids", Arrays.asList("0"))
                                ),
                                selections(
                                        field("id"),
                                        field("overallRating")
                                )
                        )
                )
        ).toGraphQLSpec();

        String expected = "Exception while fetching data (/playerStats) : Invalid operation: Filtering by ID is not supported on playerStats";

        runQueryWithExpectedError(graphQLRequest, expected);
    }

    @Test
    public void testUpdate() throws IOException {

        PlayerStats playerStats = new PlayerStats();
        playerStats.setId("1");
        playerStats.setHighScore(100);

        String graphQLRequest = mutation(
                selection(
                        field(
                                "playerStats",
                                arguments(
                                        argument("op", "UPDATE"),
                                        argument("data", playerStats)
                                ),
                                selections(
                                        field("id"),
                                        field("overallRating")
                                )
                        )
                )
        ).toGraphQLSpec();

        String expected = "Exception while fetching data (/playerStats) : Invalid operation: Filtering by ID is not supported on playerStats";

        runQueryWithExpectedError(graphQLRequest, expected);
    }

    @Test
    public void testUpsertWithStaticModel() throws IOException {

        PlayerStats playerStats = new PlayerStats();
        playerStats.setId("1");
        playerStats.setHighScore(100);

        String graphQLRequest = mutation(
                selection(
                        field(
                                "playerStats",
                                arguments(
                                        argument("op", "UPSERT"),
                                        argument("data", playerStats)
                                ),
                                selections(
                                        field("id"),
                                        field("overallRating")
                                )
                        )
                )
        ).toGraphQLSpec();

        String expected = "Exception while fetching data (/playerStats) : Invalid operation: Filtering by ID is not supported on playerStats";

        runQueryWithExpectedError(graphQLRequest, expected);
    }

    @Test
    public void testUpsertWithDynamicModel() throws IOException {

        Map<String, Object> order = new HashMap<>();
        order.put("orderId", "1");
        order.put("courierName", "foo");

        String graphQLRequest = mutation(
                selection(
                        field(
                                "SalesNamespace_orderDetails",
                                arguments(
                                        argument("op", "UPSERT"),
                                        argument("data", order)
                                ),
                                selections(
                                        field("orderId")
                                )
                        )
                )
        ).toGraphQLSpec();

        String expected = "Invalid operation: SalesNamespace_orderDetails is read only.";

        runQueryWithExpectedError(graphQLRequest, expected);
    }


    //Security
    @Test
    public void testPermissionFilters() throws IOException {
        when(securityContextMock.isUserInRole("admin.user")).thenReturn(false);

        String graphQLRequest = document(
                selection(
                        field(
                                "videoGame",
                                arguments(
                                        argument("sort", "\"timeSpentPerSession\"")
                                ),
                                selections(
                                        field("timeSpent"),
                                        field("sessions"),
                                        field("timeSpentPerSession")
                                )
                        )
                )
        ).toQuery();

        //Records for Jon Doe and Jane Doe will only be aggregated.
        String expected = document(
                selections(
                        field(
                                "videoGame",
                                selections(
                                        field("timeSpent", 1070),
                                        field("sessions", 85),
                                        field("timeSpentPerSession", 12.588235)
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);

    }

    @Test
    public void testFieldPermissions() throws IOException {
        when(securityContextMock.isUserInRole("operator")).thenReturn(false);
        String graphQLRequest = document(
                selection(
                        field(
                                "videoGame",
                                selections(
                                        field("timeSpent"),
                                        field("sessions"),
                                        field("timeSpentPerSession"),
                                        field("timeSpentPerGame")
                                )
                        )
                )
        ).toQuery();

        String expected = "Exception while fetching data (/videoGame/edges[0]/node/timeSpentPerGame) : ReadPermission Denied";

        runQueryWithExpectedError(graphQLRequest, expected);

    }
}
