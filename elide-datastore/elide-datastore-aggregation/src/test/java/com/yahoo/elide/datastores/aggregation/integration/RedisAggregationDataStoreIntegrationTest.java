/*
 * Copyright 2022, Yahoo Inc.
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
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.data;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.core.exceptions.HttpStatus;
import com.yahoo.elide.datastores.aggregation.AggregationDataStore;
import com.yahoo.elide.datastores.aggregation.framework.RedisAggregationDataStoreTestHarness;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.ConnectionDetails;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import jakarta.persistence.EntityManagerFactory;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Integration tests for {@link AggregationDataStore} using Redis for cache.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RedisAggregationDataStoreIntegrationTest extends AggregationDataStoreIntegrationTest {
    private static final int PORT = 6379;

    private RedisServer redisServer;

    public RedisAggregationDataStoreIntegrationTest() {
        super();
    }

    @BeforeAll
    public void beforeAll() {
        super.beforeAll();
        try {
            redisServer = new RedisServer(PORT);
            redisServer.start();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @AfterAll
    public void afterEverything() {
        try {
            redisServer.stop();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected DataStoreTestHarness createHarness() {

        ConnectionDetails defaultConnectionDetails = createDefaultConnectionDetails();

        EntityManagerFactory emf = createEntityManagerFactory();

        Map<String, ConnectionDetails> connectionDetailsMap = createConnectionDetailsMap(defaultConnectionDetails);

        return new RedisAggregationDataStoreTestHarness(emf, defaultConnectionDetails, connectionDetailsMap, VALIDATOR);
    }

    @Test
    public void parameterizedJsonApiColumnTest() throws Exception {
        when()
            .get("/SalesNamespace_orderDetails?filter=deliveryTime>='2020-01-01';deliveryTime<'2020-12-31'&fields[SalesNamespace_orderDetails]=orderRatio")
            .then()
            .body(equalTo(
                data(
                    resource(
                        type("SalesNamespace_orderDetails"),
                        id("0"),
                        attributes(
                            attr("orderRatio", new BigDecimal("1.0000000000000000000000000000000000000000"))
                        )
                    )
                ).toJSON())
            )
            .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void parameterizedGraphQLFilterNoAliasTest() throws Exception {
        String graphQLRequest = document(
                selection(
                        field(
                                "SalesNamespace_orderDetails",
                                arguments(
                                        argument("filter", "\"orderRatio[numerator:orderMax][denominator:orderMax]>=.5;deliveryTime>='2020-01-01';deliveryTime<'2020-12-31'\"")
                                ),
                                selections(
                                        field("orderRatio", "ratio1", arguments(
                                                argument("numerator", "\"orderMax\""),
                                                argument("denominator", "\"orderMax\"")
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
                                        field("ratio1", 1.0)
                                )
                        )
                )
        ).toResponse();


        runQueryWithExpectedResult(graphQLRequest, expected);

        // Call the Query Again to hit the cache to retrieve the results
        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void parameterizedGraphQLFilterWithAliasTest() throws Exception {
        String graphQLRequest = document(
                selection(
                        field(
                                "SalesNamespace_orderDetails",
                                arguments(
                                        argument("filter", "\"ratio1>=.5;deliveryTime>='2020-01-01';deliveryTime<'2020-12-31'\"")
                                ),
                                selections(
                                        field("orderRatio", "ratio1", arguments(
                                                argument("numerator", "\"orderMax\""),
                                                argument("denominator", "\"orderMax\"")
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
                                        field("ratio1", 1.0)
                                )
                        )
                )
        ).toResponse();


        runQueryWithExpectedResult(graphQLRequest, expected);

        // Call the Query Again to hit the cache to retrieve the results
        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    // Use Non Dynamic Model for caching
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
                                        field("highScore", 3147483647L),
                                        field("overallRating", "Great"),
                                        field("countryIsoCode", "USA"),
                                        field("playerRank", 2)
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);

        // Call the Query Again to hit the cache to retrieve the results
        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    /**
     * Below tests demonstrate using the aggregation store from dynamic configuration through JSON API.
     */
    @Test
    public void testDynamicAggregationModel() {
        String getPath = "/SalesNamespace_orderDetails?sort=customerRegion,orderTime&page[totals]&"
                        + "fields[SalesNamespace_orderDetails]=orderTotal,customerRegion,orderTime&filter=deliveryTime>=2020-01-01;deliveryTime<2020-12-31;orderTime>=2020-08";
        given()
            .when()
            .get(getPath)
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body("data", hasSize(4))
            .body("data.id", hasItems("0", "1", "2", "3"))
            .body("data.attributes", hasItems(
                    allOf(hasEntry("customerRegion", "NewYork"), hasEntry("orderTime", "2020-08")),
                    allOf(hasEntry("customerRegion", "Virginia"), hasEntry("orderTime", "2020-08")),
                    allOf(hasEntry("customerRegion", "Virginia"), hasEntry("orderTime", "2020-08")),
                    allOf(hasEntry("customerRegion", null), hasEntry("orderTime", "2020-09"))))
            .body("data.attributes.orderTotal", hasItems(78.87F, 61.43F, 113.07F, 260.34F))
            .body("meta.page.number", equalTo(1))
            .body("meta.page.totalRecords", equalTo(4))
            .body("meta.page.totalPages", equalTo(1))
            .body("meta.page.limit", equalTo(500));

        // Run the query again to hit the cache.
        given()
            .when()
            .get(getPath)
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body("data", hasSize(4))
            .body("data.id", hasItems("0", "1", "2", "3"))
            .body("data.attributes", hasItems(
                    allOf(hasEntry("customerRegion", "NewYork"), hasEntry("orderTime", "2020-08")),
                    allOf(hasEntry("customerRegion", "Virginia"), hasEntry("orderTime", "2020-08")),
                    allOf(hasEntry("customerRegion", "Virginia"), hasEntry("orderTime", "2020-08")),
                    allOf(hasEntry("customerRegion", null), hasEntry("orderTime", "2020-09"))))
            .body("data.attributes.orderTotal", hasItems(78.87F, 61.43F, 113.07F, 260.34F))
            .body("meta.page.number", equalTo(1))
            .body("meta.page.totalRecords", equalTo(4))
            .body("meta.page.totalPages", equalTo(1))
            .body("meta.page.limit", equalTo(500));
    }
}
