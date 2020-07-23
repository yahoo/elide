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
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.datastores.aggregation.AggregationDataStore;
import com.yahoo.elide.datastores.aggregation.framework.AggregationDataStoreTestHarness;
import com.yahoo.elide.datastores.aggregation.framework.SQLUnitTest;
import com.yahoo.elide.initialization.IntegrationTest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.restassured.response.ValidatableResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.stream.Collectors;
import javax.persistence.Persistence;
import javax.ws.rs.core.MediaType;

/**
 * Integration tests for {@link AggregationDataStore}.
 */
public class AggregationDataStoreIntegrationTest extends IntegrationTest {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    @BeforeAll
    public void beforeEach() {
        SQLUnitTest.init();
    }

    @Override
    protected DataStoreTestHarness createHarness() {
        return new AggregationDataStoreTestHarness(Persistence.createEntityManagerFactory("aggregationStore"));
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
                                        field("recordedDate",
                                                arguments(
                                                        argument("grain", "\"MONTH\"")
                                                )
                                        ),
                                        field("updatedDate",
                                                arguments(
                                                        argument("grain", "\"DAY\"")
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
                                        field("recordedDate", "2019-07-01T00:00Z"),
                                        field("updatedDate", "2019-10-12T00:00Z")
                                ),
                                selections(
                                        field("recordedDate", "2019-07-01T00:00Z"),
                                        field("updatedDate", "2020-07-12T00:00Z")
                                ),
                                selections(
                                        field("recordedDate", "2019-07-01T00:00Z"),
                                        field("updatedDate", "2020-01-12T00:00Z")
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

    @Test
    public void tableMetaDataTest() {

        given()
                .accept("application/vnd.api+json")
                .get("/table/country")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.cardinality", equalTo("SMALL"))
                .body("data.relationships.columns.data.id", hasItems("country.id", "country.name", "country.isoCode"));
        given()
                .accept("application/vnd.api+json")
                .get("/table/playerStats")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.cardinality", equalTo("LARGE"))
                .body("data.attributes.category", equalTo("Sports Category"))
                .body(
                        "data.relationships.dimensions.data.id",
                        hasItems(
                                "playerStats.id",
                                "playerStats.playerName",
                                "playerStats.player2Name",
                                "playerStats.countryIsoCode",
                                "playerStats.subCountryIsoCode",
                                "playerStats.overallRating"))
                .body("data.relationships.metrics.data.id", hasItems("playerStats.lowScore", "playerStats.highScore"))
                .body("data.relationships.timeDimensions.data.id", hasItems("playerStats.recordedDate"));
    }

    @Test
    public void dimensionMetaDataTest() {

        given()
                .accept("application/vnd.api+json")
                .get("/dimension/playerStats.playerName")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.name", equalTo("playerName"))
                .body("data.attributes.valueType",  equalTo("TEXT"))
                .body("data.attributes.columnType",  equalTo("REFERENCE"))
                .body("data.attributes.expression",  equalTo("player.name"))
                .body("data.relationships.table.data.id", equalTo("playerStats"));

    }

    @Test
    public void timeDimensionMetaDataTest() {

        given()
                .accept("application/vnd.api+json")
                .get("/timeDimension/playerStats.recordedDate?include=supportedGrains")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.name", equalTo("recordedDate"))
                .body("data.attributes.valueType",  equalTo("TIME"))
                .body("data.attributes.columnType",  equalTo("FIELD"))
                .body("data.attributes.expression",  equalTo("recordedDate"))
                .body("data.relationships.table.data.id", equalTo("playerStats"))
                .body(
                        "data.relationships.supportedGrains.data.id",
                        hasItems("playerStats.recordedDate.day", "playerStats.recordedDate.month"))
                .body("included.id", hasItems("playerStats.recordedDate.day", "playerStats.recordedDate.month"))
                .body("included.attributes.grain", hasItems("DAY", "MONTH"))
                .body(
                        "included.attributes.expression",
                        hasItems(
                                "PARSEDATETIME(FORMATDATETIME({{}}, 'yyyy-MM-dd'), 'yyyy-MM-dd')",
                                "PARSEDATETIME(FORMATDATETIME({{    }}, 'yyyy-MM-01'), 'yyyy-MM-dd')"));
    }
    @Test
    public void metricMetaDataTest() {

        given()
                .accept("application/vnd.api+json")
                .get("/metric/playerStats.lowScore?include=metricFunction")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.name", equalTo("lowScore"))
                .body("data.attributes.valueType",  equalTo("INTEGER"))
                .body("data.attributes.columnType",  equalTo("FIELD"))
                .body("data.attributes.expression",  equalTo("lowScore"))
                .body("data.attributes.category",  equalTo("Score Category"))
                .body("data.attributes.description",  equalTo("very low score"))
                .body("data.relationships.table.data.id", equalTo("playerStats"))
                .body("data.relationships.metricFunction.data.id", equalTo("playerStats.lowScore[min]"))
                .body("included.id", hasItem("playerStats.lowScore[min]"))
                .body("included.attributes.description", hasItem("very low score"))
                .body("included.attributes.expression", hasItem("MIN(%s)"));

        given()
                .accept("application/vnd.api+json")
                .get("/metric/videoGame.timeSpentPerSession")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.name", equalTo("timeSpentPerSession"))
                .body("data.attributes.valueType",  equalTo("DECIMAL"))
                .body("data.attributes.columnType",  equalTo("FORMULA"))
                .body("data.attributes.expression",  equalTo("({{timeSpent}} / (CASE WHEN SUM({{game_rounds}}) = 0 THEN 1 ELSE {{sessions}} END))"))
                .body("data.relationships.table.data.id", equalTo("videoGame"));

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
                .post("/graphQL")
                .then();

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

    public String loadGraphQLResponse(String fileName) throws IOException {
        try (InputStream in = AggregationDataStoreIntegrationTest.class.getResourceAsStream("/graphql/responses/" + fileName)) {
            return new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n"));
        }
    }
}
