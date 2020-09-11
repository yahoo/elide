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
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.hasItems;

import com.yahoo.elide.contrib.dynamicconfighelpers.compile.ConnectionDetails;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.datastores.aggregation.AggregationDataStore;
import com.yahoo.elide.datastores.aggregation.framework.AggregationDataStoreTestHarness;
import com.yahoo.elide.datastores.aggregation.framework.SQLUnitTest;
import com.yahoo.elide.initialization.GraphQLIntegrationTest;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.sql.DataSource;

/**
 * Integration tests for {@link AggregationDataStore}.
 */
public class AggregationDataStoreIntegrationTest extends GraphQLIntegrationTest {

    @BeforeAll
    public void beforeAll() {
        SQLUnitTest.init();
    }

    @Override
    protected DataStoreTestHarness createHarness() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("aggregationStore");

        HikariConfig config = new HikariConfig();
        config.setDriverClassName(emf.getProperties().get("javax.persistence.jdbc.driver").toString());
        config.setJdbcUrl(emf.getProperties().get("javax.persistence.jdbc.url").toString());
        DataSource defaultDataSource = new HikariDataSource(config);
        String defaultDialect = "com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.impl.H2Dialect";

        Map<String, ConnectionDetails> connectionDetailsMap = new HashMap<>();
        // Add an entry for "mycon" connection
        connectionDetailsMap.put("mycon", new ConnectionDetails(defaultDataSource, defaultDialect));

        return new AggregationDataStoreTestHarness(emf, defaultDataSource, defaultDialect, connectionDetailsMap);
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
                                        field("highScore", 1000),
                                        field("overallRating", "Good"),
                                        field("countryIsoCode", "HKG")
                                ),
                                selections(
                                        field("highScore", 1234),
                                        field("overallRating", "Good"),
                                        field("countryIsoCode", "USA")
                                ),
                                selections(
                                        field("highScore", 2412),
                                        field("overallRating", "Great"),
                                        field("countryIsoCode", "USA")
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
                + "Can not filter on relationship field [PlayerStats].country/[Country].isoCode in HAVING clause "
                + "when querying table PlayerStats.\"";

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
                                        field("recordedDate", "2019-07-13T00:00Z"),
                                        field("updatedDate", "2020-01-12T00:00Z")
                                ),
                                selections(
                                        field("recordedDate", "2019-07-12T00:00Z"),
                                        field("updatedDate", "2019-10-12T00:00Z")
                                ),
                                selections(
                                        field("recordedDate", "2019-07-11T00:00Z"),
                                        field("updatedDate", "2020-07-12T00:00Z")
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
}
