/*
 * Copyright 2019, Yahoo Inc.
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
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.datastores.aggregation.AggregationDataStore;
import com.yahoo.elide.datastores.aggregation.framework.AggregationDataStoreTestHarness;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngineFactory;
import com.yahoo.elide.initialization.IntegrationTest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.restassured.response.ValidatableResponse;

import java.io.IOException;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.ws.rs.core.MediaType;

/**
 * Integration tests for {@link AggregationDataStore}.
 */
public class AggregationDataStoreIntegrationTest extends IntegrationTest {
    SQLQueryEngineFactory queryEngineFactory;

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    @Override
    protected DataStoreTestHarness createHarness() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("aggregationStore");
        queryEngineFactory = new SQLQueryEngineFactory(emf);
        return new AggregationDataStoreTestHarness(queryEngineFactory);
    }

    @Test
    public void basicAggregationTest() throws Exception {
        String graphQLRequest = document(
                selection(
                        field(
                                "playerStats",
                                selections(
                                        field("highScore"),
                                        field("overallRating"),
                                        field(
                                                "country",
                                                selections(
                                                        field("name")
                                                )
                                        )
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
                                        field("overallRating", "Good"),
                                        field(
                                                "country",
                                                selections(
                                                        field("name", "United States")
                                                )
                                        )
                                ),
                                selections(
                                        field("highScore", 2412),
                                        field("overallRating", "Great"),
                                        field(
                                                "country",
                                                selections(
                                                        field("name", "United States")
                                                )
                                        )
                                ),
                                selections(
                                        field("highScore", 1000),
                                        field("overallRating", "Good"),
                                        field(
                                                "country",
                                                selections(
                                                        field("name", "Hong Kong")
                                                )
                                        )
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
                                        field(
                                                "player",
                                                selections(
                                                        field("name")
                                                )
                                        )
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
                                        field(
                                                "player",
                                                selections(
                                                        field("name", "Jon Doe")
                                                )
                                        )
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
                                        argument("filter", "\"overallRating==\\\"Good\\\",lowScore<\\\"45\\\"\"")
                                ),
                                selections(
                                        field("lowScore"),
                                        field("overallRating"),
                                        field(
                                                "player",
                                                selections(
                                                        field("name")
                                                )
                                        )
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
                                        field(
                                                "player",
                                                selections(
                                                        field("name", "Jon Doe")
                                                )
                                        )
                                ),
                                selections(
                                        field("lowScore", 72),
                                        field("overallRating", "Good"),
                                        field(
                                                "player",
                                                selections(
                                                        field("name", "Han")
                                                )
                                        )
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
                                        field(
                                                "player",
                                                selections(
                                                        field("name")
                                                )
                                        )
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
                                        field(
                                                "player",
                                                selections(
                                                        field("name", "Jon Doe")
                                                )
                                        )
                                ),
                                selections(
                                        field("lowScore", 241),
                                        field("countryIsoCode", "USA"),
                                        field(
                                                "player",
                                                selections(
                                                        field("name", "Jane Doe")
                                                )
                                        )
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
                + "'Dimension field countryIsoCode must be grouped before filtering in having clause.'\"";

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
                + "'Metric field highScore must be aggregated before filtering in having clause.'\"";

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
                + "'Can't filter on relationship field [PlayerStats].country/[Country].isoCode in HAVING clause "
                + "when querying table PlayerStats.'\"";

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
                                        field(
                                                "country",
                                                selections(
                                                        field("name")
                                                )
                                        )
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
                                        field(
                                                "country",
                                                selections(
                                                        field("name", "United States")
                                                )
                                        )
                                ),
                                selections(
                                        field("highScore", 1000),
                                        field(
                                                "country",
                                                selections(
                                                        field("name", "Hong Kong")
                                                )
                                        )
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
                                        argument("sort", "\"overallRating,player.name\"")
                                ),
                                selections(
                                        field("lowScore"),
                                        field("overallRating"),
                                        field(
                                                "player",
                                                selections(
                                                        field("name")
                                                )
                                        )
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
                                        field(
                                                "player",
                                                selections(
                                                        field("name", "Han")
                                                )
                                        )
                                ),
                                selections(
                                        field("lowScore", 35),
                                        field("overallRating", "Good"),
                                        field(
                                                "player",
                                                selections(
                                                        field("name", "Jon Doe")
                                                )
                                        )
                                ),
                                selections(
                                        field("lowScore", 241),
                                        field("overallRating", "Great"),
                                        field(
                                                "player",
                                                selections(
                                                        field("name", "Jane Doe")
                                                )
                                        )
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

        String expected = "\"Exception while fetching data (/playerStats) : Invalid operation: 'Sorting on id field is not permitted'\"";

        runQueryWithExpectedError(graphQLRequest, expected);
    }

    @Test
    public void nestedDimensionNotInQuerySortingTest() throws Exception {
        String graphQLRequest = document(
                selection(
                        field(
                                "playerStats",
                                arguments(
                                        argument("sort", "\"-country.name,lowScore\"")
                                ),
                                selections(
                                        field("lowScore")
                                )
                        )
                )
        ).toQuery();

        String expected = "\"Exception while fetching data (/playerStats) : Invalid operation: 'Can't sort on country as it is not present in query'\"";

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
                                        field(
                                                "country",
                                                selections(
                                                        field("name")
                                                )
                                        )
                                )
                        )
                )
        ).toQuery();

        String expected = "\"Exception while fetching data (/playerStats) : Invalid operation: 'Can't sort on highScore as it is not present in query'\"";

        runQueryWithExpectedError(graphQLRequest, expected);
    }

    @Test
    public void noMetricQueryTest() throws Exception {
        String graphQLRequest = document(
                selection(
                        field(
                                "playerStats",
                                selections(
                                        field(
                                                "country",
                                                selections(
                                                        field("name")
                                                )
                                        )
                                )
                        )
                )
        ).toQuery();

        String expected = "\"Exception while fetching data (/playerStats) : Invalid operation: 'Must provide at least one metric in query'\"";

        runQueryWithExpectedError(graphQLRequest, expected);
    }

    @Test
    public void sortingMultipleLevelNesting() throws Exception {
        String graphQLRequest = document(
                selection(
                        field(
                                "playerStats",
                                arguments(
                                        argument("sort", "\"country.continent.name\"")
                                ),
                                selections(
                                        field("lowScore"),
                                        field(
                                                "country",
                                                selections(
                                                        field("name"),
                                                        field(
                                                                "continent",
                                                                selections(
                                                                        field("name")
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        ).toQuery();

        String expected = "\"Exception while fetching data (/playerStats) : Currently sorting on double nested fields is not supported\"";

        runQueryWithExpectedError(graphQLRequest, expected);
    }

    @Test
    @Disabled
    //FIXME Needs metric computation support for test case to be valid.
    public void aggregationComputedMetricTest() throws Exception {
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

        String expected = document(
                selections(
                        field(
                                "videoGame",
                                selections(
                                        field("timeSpent", 1400),
                                        field("sessions", 70),
                                        field("timeSpentPerSession", 20),
                                        field("timeSpentPerGame", 14)
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void basicViewAggregationTest() throws Exception {
        String graphQLRequest = document(
                selection(
                        field(
                                "playerStatsWithView",
                                selections(
                                        field("highScore"),
                                        field(
                                                "country",
                                                selections(
                                                        field("name")
                                                )
                                        ),
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
                                        field(
                                                "country",
                                                selections(
                                                        field("name", "Hong Kong")
                                                )
                                        ),
                                        field("countryViewIsoCode", "HKG")
                                ),
                                selections(
                                        field("highScore", 2412),
                                        field(
                                                "country",
                                                selections(
                                                        field("name", "United States")
                                                )
                                        ),
                                        field("countryViewIsoCode", "USA")
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
                .body("data.relationships.country.data.id", hasItems("840", "344"));
    }

    @Test
    public void metaDataTest() {
        given()
                .accept("application/vnd.api+json")
                .get("/table/country")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.cardinality", equalTo("SMALL"))
                .body("data.relationships.columns.data.id", hasItems("country.id", "country.name", "country.isoCode"));

        given()
                .accept("application/vnd.api+json")
                .get("/analyticView/playerStats")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.cardinality", equalTo("LARGE"))
                .body(
                        "data.relationships.dimensions.data.id",
                        hasItems(
                                "playerStats.id",
                                "playerStats.player",
                                "playerStats.country",
                                "playerStats.subCountry",
                                "playerStats.recordedDate",
                                "playerStats.overallRating",
                                "playerStats.countryIsoCode",
                                "playerStats.subCountryIsoCode"))
                .body("data.relationships.metrics.data.id", hasItems("playerStats.lowScore", "playerStats.highScore"));

        given()
                .accept("application/vnd.api+json")
                .get("/dimension/playerStats.player")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.name", equalTo("player"))
                .body("data.attributes.tableName", equalTo("playerStats"))
                .body("data.relationships.dataType.data.id", equalTo("player"));

        given()
                .accept("application/vnd.api+json")
                .get("/metric/playerStats.lowScore?include=metricFunction")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.name", equalTo("lowScore"))
                .body("data.attributes.tableName", equalTo("playerStats"))
                .body("data.relationships.dataType.data.id", equalTo("p_bigint"))
                .body("data.relationships.metricFunction.data.id", equalTo("playerStats.lowScore[min]"))
                .body("included.id", hasItem("playerStats.lowScore[min]"))
                .body("included.attributes.description", hasItem("sql min function"))
                .body("included.attributes.expression", hasItem("MIN(lowScore)"))
                .body("included.attributes.longName", hasItem("min"));

        given()
                .accept("application/vnd.api+json")
                .get("/timeDimension/playerStats.recordedDate?include=supportedGrains")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.name", equalTo("recordedDate"))
                .body("data.attributes.tableName", equalTo("playerStats"))
                .body("data.relationships.dataType.data.id", equalTo("date"))
                .body(
                        "data.relationships.supportedGrains.data.id",
                        hasItems("playerStats.recordedDate.day", "playerStats.recordedDate.month"))
                .body("included.id", hasItems("playerStats.recordedDate.day", "playerStats.recordedDate.month"))
                .body("included.attributes.grain", hasItems("DAY", "MONTH"))
                .body(
                        "included.attributes.expression",
                        hasItems(
                                "PARSEDATETIME(FORMATDATETIME(%s, 'yyyy-MM-dd'), 'yyyy-MM-dd')",
                                "PARSEDATETIME(FORMATDATETIME(%s, 'yyyy-MM-01'), 'yyyy-MM-dd')"));
    }

    private void create(String query, Map<String, Object> variables) throws IOException {
        runQuery(toJsonQuery(query, variables));
    }

    private void runQueryWithExpectedResult(
            String graphQLQuery,
            Map<String, Object> variables,
            String expected
    ) throws IOException {
        compareJsonObject(runQuery(graphQLQuery, variables), expected);
    }

    private void runQueryWithExpectedResult(String graphQLQuery, String expected) throws IOException {
        runQueryWithExpectedResult(graphQLQuery, null, expected);
    }

    private void runQueryWithExpectedError(
            String graphQLQuery,
            Map<String, Object> variables,
            String errorMessage
    ) throws IOException {
        compareErrorMessage(runQuery(graphQLQuery, variables), errorMessage);
    }

    private void runQueryWithExpectedError(String graphQLQuery, String errorMessage) throws IOException {
        runQueryWithExpectedError(graphQLQuery, null, errorMessage);
    }

    private void compareJsonObject(ValidatableResponse response, String expected) throws IOException {
        JsonNode responseNode = JSON_MAPPER.readTree(response.extract().body().asString());
        JsonNode expectedNode = JSON_MAPPER.readTree(expected);
        assertEquals(expectedNode, responseNode);
    }

    private void compareErrorMessage(ValidatableResponse response, String expected) throws IOException {
        JsonNode responseNode = JSON_MAPPER.readTree(response.extract().body().asString());
        assertEquals(expected, responseNode.get("errors").get(0).get("message").toString());
    }

    private ValidatableResponse runQuery(String query, Map<String, Object> variables) throws IOException {
        return runQuery(toJsonQuery(query, variables));
    }

    private ValidatableResponse runQuery(String query) {
        ValidatableResponse res = given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(query)
                .log().all()
                .post("/graphQL")
                .then()
                .log()
                .all();

        return res;
    }

    private String toJsonArray(JsonNode... nodes) throws IOException {
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        for (JsonNode node : nodes) {
            arrayNode.add(node);
        }
        return JSON_MAPPER.writeValueAsString(arrayNode);
    }

    private String toJsonQuery(String query, Map<String, Object> variables) throws IOException {
        return JSON_MAPPER.writeValueAsString(toJsonNode(query, variables));
    }

    private JsonNode toJsonNode(String query) {
        return toJsonNode(query, null);
    }

    private JsonNode toJsonNode(String query, Map<String, Object> variables) {
        ObjectNode graphqlNode = JsonNodeFactory.instance.objectNode();
        graphqlNode.put("query", query);
        if (variables != null) {
            graphqlNode.set("variables", JSON_MAPPER.valueToTree(variables));
        }
        return graphqlNode;
    }
}
