package com.yahoo.elide.datastores.aggregation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.datastores.aggregation.dimension.TimeDimension;
import com.yahoo.elide.datastores.aggregation.engine.SQLQueryEngine;
import com.yahoo.elide.datastores.aggregation.engine.schema.SQLSchema;
import com.yahoo.elide.datastores.aggregation.example.Country;
import com.yahoo.elide.datastores.aggregation.example.Player;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.example.PlayerStatsView;
import com.yahoo.elide.datastores.aggregation.example.VideoGame;
import com.yahoo.elide.datastores.aggregation.metric.Metric;
import com.yahoo.elide.datastores.aggregation.metric.Sum;
import com.yahoo.elide.datastores.aggregation.schema.Schema;
import com.yahoo.elide.initialization.IntegrationTest;
import com.yahoo.elide.request.Attribute;
import com.yahoo.elide.request.EntityProjection;
import io.restassured.response.ValidatableResponse;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.internal.matchers.apachecommons.ReflectionEquals;
import org.testng.Assert;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.*;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.field;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AggregationDataStoreTest extends IntegrationTest {
    @Spy
    AggregationDataStore aggregationDataStore;

    @Mock
    EntityProjection entityProjection;

    @Mock
    RequestScope requestScope;

    @Spy
    EntityDictionary entityDictionary;

    QueryEngine qE;

    private EntityManagerFactory emf;

    private Schema playerStatsSchema;
    private Schema playerStatsViewSchema;
    private RSQLFilterDialect filterParser;

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    @Override
    protected DataStoreTestHarness createHarness() {
        entityDictionary = new EntityDictionary(new HashMap<>());
        entityDictionary.bindEntity(PlayerStats.class);
        entityDictionary.bindEntity(Country.class);
        entityDictionary.bindEntity(PlayerStatsView.class);
        entityDictionary.bindEntity(Player.class);
        entityDictionary.bindEntity(VideoGame.class);

        emf = Persistence.createEntityManagerFactory("aggregationStore");
        EntityManager em = emf.createEntityManager();
        qE = new SQLQueryEngine(em, entityDictionary);
        return new AggregationDataStoreTestHarness(qE);
    }

    AggregationDataStoreTest() {
        EntityManager em = emf.createEntityManager();
        qE = new SQLQueryEngine(em, entityDictionary);
    }
    @BeforeEach
    public void init() {
        emf = Persistence.createEntityManagerFactory("aggregationStore");
        EntityManager em = emf.createEntityManager();
        entityDictionary = new EntityDictionary(new HashMap<>());
        MockitoAnnotations.initMocks(this);
        entityDictionary.bindEntity(PlayerStats.class);
        entityDictionary.bindEntity(Country.class);
        entityDictionary.bindEntity(PlayerStatsView.class);
        entityDictionary.bindEntity(Player.class);
        entityDictionary.bindEntity(VideoGame.class);

        filterParser = new RSQLFilterDialect(entityDictionary);

        playerStatsSchema = new SQLSchema(PlayerStats.class, entityDictionary);
        playerStatsViewSchema = new SQLSchema(PlayerStatsView.class, entityDictionary);
        qE = new SQLQueryEngine(em, entityDictionary);
    }

    @Test
    public void testingSQLQueryEngine() {
        EntityManager em = emf.createEntityManager();
        qE = new SQLQueryEngine(em, entityDictionary);
        Query query = Query.builder()
                .schema(playerStatsSchema)
                .metric(playerStatsSchema.getMetric("lowScore"), Sum.class)
                .metric(playerStatsSchema.getMetric("highScore"), Sum.class)
                .groupDimension(playerStatsSchema.getDimension("overallRating"))
                .timeDimension((TimeDimension) playerStatsSchema.getDimension("recordedDate"))
                .build();

        List<Object> results = StreamSupport.stream(qE.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        //Jon Doe,1234,72,Good,840,2019-07-12 00:00:00
        PlayerStats stats1 = new PlayerStats();
        stats1.setId("0");
        stats1.setLowScore(72);
        stats1.setHighScore(1234);
        stats1.setOverallRating("Good");
        stats1.setRecordedDate(Timestamp.valueOf("2019-07-12 00:00:00"));

        PlayerStats stats2 = new PlayerStats();
        stats2.setId("1");
        stats2.setLowScore(241);
        stats2.setHighScore(2412);
        stats2.setOverallRating("Great");
        stats2.setRecordedDate(Timestamp.valueOf("2019-07-11 00:00:00"));

        Assert.assertEquals(results.size(), 2);
        Assert.assertEquals(results.get(0), stats1);
        Assert.assertEquals(results.get(1), stats2);
    }

    @Test
    public void aggregationMaxTest() throws Exception {
        String graphQLRequest = document(
                selection(
                        field(
                                "playerStats",
                                selections(
                                        field("highScore"),
                                        field(
                                                "player",
                                                selections(
                                                        field("name"),
                                                        field("id")
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
                                        field("highScore", 681L),
                                        field(
                                                "country",
                                                selections(
                                                        field("name", "Germany")
                                                )
                                        )
                                ),
                                selections(
                                        field("highScore", 421L),
                                        field(
                                                "country",
                                                selections(
                                                        field("name", "Italy")
                                                )
                                        )
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void aggregationFilterTest() throws Exception {
        String graphQLRequest = document(
                selection(
                        field(
                                "playerStats",
                                arguments(
                                        argument("filter", "\"highScore<\\\"1500\\\"\"")
                                ),
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
                                        field("highScore", 681L),
                                        field(
                                                "country",
                                                selections(
                                                        field("name", "Germany")
                                                )
                                        )
                                ),
                                selections(
                                        field("highScore", 421L),
                                        field(
                                                "country",
                                                selections(
                                                        field("name", "Italy")
                                                )
                                        )
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void aggregationComputedMetricTest() throws Exception {
        String graphQLRequest = document(
                selection(
                        field(
                                "videoGame",
                                selections(
                                        field("timeSpent")
                                )
                        )
                )
        ).toQuery();

        String expected = document(
                selections(
                        field(
                                "playerStats",
                                selections(
                                        field("highScore", 681L),
                                        field(
                                                "country",
                                                selections(
                                                        field("name", "Germany")
                                                )
                                        )
                                ),
                                selections(
                                        field("highScore", 421L),
                                        field(
                                                "country",
                                                selections(
                                                        field("name", "Italy")
                                                )
                                        )
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
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

    private void compareJsonObject(ValidatableResponse response, String expected) throws IOException {
        JsonNode responseNode = JSON_MAPPER.readTree(response.extract().body().asString());
        JsonNode expectedNode = JSON_MAPPER.readTree(expected);
        assertEquals(expectedNode, responseNode);
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
