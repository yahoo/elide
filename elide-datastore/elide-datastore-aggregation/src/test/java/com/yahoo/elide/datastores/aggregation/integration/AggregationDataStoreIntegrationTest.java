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
import static com.yahoo.elide.test.graphql.GraphQLDSL.selection;
import static com.yahoo.elide.test.graphql.GraphQLDSL.selections;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.annotation.SecurityCheck;
import com.yahoo.elide.core.audit.TestAuditLogger;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.HttpStatus;
import com.yahoo.elide.core.security.checks.Check;
import com.yahoo.elide.datastores.aggregation.AggregationDataStore;
import com.yahoo.elide.datastores.aggregation.checks.OperatorCheck;
import com.yahoo.elide.datastores.aggregation.framework.AggregationDataStoreTestHarness;
import com.yahoo.elide.datastores.aggregation.framework.SQLUnitTest;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.ConnectionDetails;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.DataSourceConfiguration;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialect;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.yahoo.elide.initialization.GraphQLIntegrationTest;
import com.yahoo.elide.jsonapi.resources.JsonApiEndpoint;
import com.yahoo.elide.modelconfig.DBPasswordExtractor;
import com.yahoo.elide.modelconfig.compile.ElideDynamicEntityCompiler;
import com.yahoo.elide.modelconfig.model.DBConfig;
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
import java.util.Base64;
import java.util.Calendar;
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
 * Integration tests for {@link AggregationDataStore}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AggregationDataStoreIntegrationTest extends GraphQLIntegrationTest {

    @Mock private static SecurityContext securityContextMock;

    public static final ElideDynamicEntityCompiler COMPILER = getCompiler("src/test/resources/configs");
    private static final class SecurityHjsonIntegrationTestResourceConfig extends ResourceConfig {

        @Inject
        public SecurityHjsonIntegrationTestResourceConfig() {
            register(new AbstractBinder() {
                @Override
                protected void configure() {
                    Map<String, Class<? extends Check>> map = new HashMap<>(TestCheckMappings.MAPPINGS);
                    map.put(OperatorCheck.OPERTOR_CHECK, OperatorCheck.class);
                    EntityDictionary dictionary = new EntityDictionary(map);

                    try {
                        dictionary.addSecurityChecks(COMPILER.findAnnotatedClasses(SecurityCheck.class));
                    } catch (ClassNotFoundException e) {
                    }

                    Elide elide = new Elide(new ElideSettingsBuilder(getDataStore())
                                    .withEntityDictionary(dictionary)
                                    .withAuditLogger(new TestAuditLogger())
                                    .withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", Calendar.getInstance().getTimeZone())
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

    public AggregationDataStoreIntegrationTest() {
        super(SecurityHjsonIntegrationTestResourceConfig.class, JsonApiEndpoint.class.getPackage().getName());
    }

    @BeforeAll
    public void beforeAll() {
        SQLUnitTest.init();
    }

    @BeforeEach
    public void setUp() {
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
        COMPILER.getElideSQLDBConfig().getDbconfigs().forEach(dbConfig -> {
            connectionDetailsMap.put(dbConfig.getName(),
                            new ConnectionDetails(getDataSource(dbConfig, getDBPasswordExtractor()),
                                            SQLDialectFactory.getDialect(dbConfig.getDialect())));
        });

        return new AggregationDataStoreTestHarness(emf, defaultConnectionDetails, connectionDetailsMap, COMPILER);
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

    private static ElideDynamicEntityCompiler getCompiler(String path) {
        try {
            return new ElideDynamicEntityCompiler(path);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
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
                                        field("timeSpent", 200),
                                        field("sessions", 10),
                                        field("timeSpentPerSession", 20.0),
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

        String errorMessage = "\"Exception while fetching data (/playerStats) : Invalid operation: "
                + "Dimension field countryIsoCode must be grouped before filtering in having clause.\"";

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
                                        argument("filter", "\"highScore<\\\"45\\\"\"")
                                ),
                                selections(
                                        field("lowScore")
                                )
                        )
                )
        ).toQuery();

        String errorMessage = "\"Exception while fetching data (/playerStats) : Invalid operation: "
                + "Metric field highScore must be aggregated before filtering in having clause.\"";

        runQueryWithExpectedError(graphQLRequest, errorMessage);
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

        String errorMessage = "\"Exception while fetching data (/playerStats) : Invalid operation: "
                + "Relationship traversal not supported for analytic queries.\"";

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

        String expected = "\"Exception while fetching data (/playerStats) : Invalid operation: Sorting on id field is not permitted\"";

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

        String expected = "\"Exception while fetching data (/playerStats) : Invalid operation: Can not sort on countryIsoCode as it is not present in query\"";

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

        String expected = "\"Exception while fetching data (/playerStats) : Invalid operation: Can not sort on highScore as it is not present in query\"";

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
    public void jsonApiAggregationTest() {
        given()
                .accept("application/vnd.api+json")
                .get("/playerStats")
                .then()
                .log().all()
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
        String getPath = "/orderDetails?sort=customerRegion,orderMonth&"
                        + "fields[orderDetails]=orderTotal,customerRegion,orderMonth&filter=orderMonth>=2020-08";
        given()
            .when()
            .get(getPath)
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body("data", hasSize(3))
            .body("data.id", hasItems("0", "1", "2"))
            .body("data.attributes", hasItems(
                            allOf(hasEntry("customerRegion", "NewYork"), hasEntry("orderMonth", "2020-08")),
                            allOf(hasEntry("customerRegion", "Virginia"), hasEntry("orderMonth", "2020-08")),
                            allOf(hasEntry("customerRegion", "Virginia"), hasEntry("orderMonth", "2020-09"))))
            .body("data.attributes.orderTotal", hasItems(61.43F, 113.07F, 260.34F));
    }

    @Test
    public void missingClientFilterTest() {
        String expectedError = "Querying deliveryDetails requires a mandatory filter:"
                + " month&gt;={{start}};month&lt;{{end}}";
        when()
        .get("/deliveryDetails/")
        .then()
        .body("errors.detail", hasItems(expectedError))
        .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void incompleteClientFilterTest() {
        String expectedError = "Querying deliveryDetails requires a mandatory filter:"
                + " month&gt;={{start}};month&lt;{{end}}";
        when()
        .get("/deliveryDetails?month&gt;=2020-08")
        .then()
        .body("errors.detail", hasItems(expectedError))
        .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void testGraphQLDynamicAggregationModel() throws Exception {
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
                                        field("customerRegionRegion"),
                                        field("orderMonth")
                                )
                        )
                )
        ).toQuery();

        String expected = document(
                selections(
                        field(
                                "orderDetails",
                                selections(
                                        field("orderTotal", 61.43F),
                                        field("customerRegion", "NewYork"),
                                        field("customerRegionRegion", "NewYork"),
                                        field("orderMonth", "2020-08")
                                ),
                                selections(
                                        field("orderTotal", 113.07F),
                                        field("customerRegion", "Virginia"),
                                        field("customerRegionRegion", "Virginia"),
                                        field("orderMonth", "2020-08")
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
                                "orderDetails",
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
                                        field("orderTime"),
                                        field("orderDate"),
                                        field("orderMonth"),
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
                                "orderDetails",
                                selections(
                                        field("courierName", "FEDEX"),
                                        field("deliveryTime", "2020-09-11T16:30:11"),
                                        field("deliveryHour", "2020-09-11T16"),
                                        field("deliveryDate", "2020-09-11"),
                                        field("deliveryMonth", "2020-09"),
                                        field("deliveryYear", "2020"),
                                        field("orderTime", "2020-09-08T16:30:11"),
                                        field("deliveryDefault", "2020-09-11"),
                                        field("orderDate", "2020-09-08"),
                                        field("orderMonth", "2020-09"),
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
                                        field("orderTime", "2020-09-08T16:30:11"),
                                        field("deliveryDefault", "2020-09-11"),
                                        field("orderDate", "2020-09-08"),
                                        field("orderMonth", "2020-09"),
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
                                        field("orderTime", "2020-08-30T16:30:11"),
                                        field("deliveryDefault", "2020-09-05"),
                                        field("orderDate", "2020-08-30"),
                                        field("orderMonth", "2020-08"),
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
                                        field("orderTime", "2020-09-09T16:30:11"),
                                        field("deliveryDefault", "2020-09-13"),
                                        field("orderDate", "2020-09-09"),
                                        field("orderMonth", "2020-09"),
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
                                "orderDetails",
                                arguments(
                                        argument("sort", "\"customerRegion\""),
                                        argument("filter", "\"orderTime=='2020-09-08T16:30:11'\"")
                                ),
                                selections(
                                        field("orderTotal"),
                                        field("customerRegion"),
                                        field("orderMonth"),
                                        field("orderTime")
                                )
                        )
                )
        ).toQuery();

        String expected = document(
                selections(
                        field(
                                "orderDetails",
                                selections(
                                        field("orderTotal", 181.47F),
                                        field("customerRegion", "Virginia"),
                                        field("orderMonth", "2020-09"),
                                        field("orderTime", "2020-09-08T16:30:11")
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
