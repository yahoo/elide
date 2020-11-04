/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.integration;

import static com.yahoo.elide.datastores.aggregation.integration.AggregationDataStoreIntegrationTest.COMPILER;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

import com.yahoo.elide.contrib.dynamicconfighelpers.compile.ConnectionDetails;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.datastores.aggregation.framework.AggregationDataStoreTestHarness;
import com.yahoo.elide.initialization.IntegrationTest;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.sql.DataSource;

public class MetaDataStoreIntegrationTest extends IntegrationTest {

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

        // Add an entry for "mycon" connection which is not from hjson
        connectionDetailsMap.put("mycon", defaultConnectionDetails);
        // Add connection details fetched from hjson
        connectionDetailsMap.putAll(COMPILER.getConnectionDetailsMap());

        return new AggregationDataStoreTestHarness(emf, defaultConnectionDetails, connectionDetailsMap, COMPILER);
    }

    @Test
    public void tableWithRequiredFilter() {
        given()
                .accept("application/vnd.api+json")
                .get("/table/playerStatsFiltered")
                .then()
                .log().all()
                .body("data.attributes.requiredFilter", equalTo("recordedDate>={{start}};recordedDate<{{end}}"))
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void tableMetaDataTest() {

        given()
               .accept("application/vnd.api+json")
               .get("/table/continent")
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("data.attributes.isFact", equalTo(false)); //TableMeta Present, isFact false
        given()
                .accept("application/vnd.api+json")
                .get("/table/playerRanking") //Entity Annotated
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.cardinality", equalTo("MEDIUM"));
        given()
                .accept("application/vnd.api+json")
                .get("/table/country")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.isFact", equalTo(false)) //TableMeta Present, isFact default true
                .body("data.attributes.cardinality", nullValue())
                .body("data.relationships.columns.data.id", hasItems("country.id", "country.name", "country.isoCode"));
        given()
                .accept("application/vnd.api+json")
                .get("/table/playerStatsView")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.isFact", equalTo(true)); //FromSubquery
        given()
                .accept("application/vnd.api+json")
                .get("/table/playerStats")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.isFact", equalTo(true)) //FromTable
                .body("data.attributes.cardinality", equalTo("LARGE"))
                .body("data.attributes.category", equalTo("Sports Category"))
                .body("data.attributes.tags", hasItems("Statistics", "Game"))
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
                .get("/table/playerStats/dimensions/playerStats.playerName")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.name", equalTo("playerName"))
                .body("data.attributes.valueType",  equalTo("TEXT"))
                .body("data.attributes.columnType",  equalTo("FORMULA"))
                .body("data.attributes.valueSourceType",  equalTo("NONE"))
                .body("data.attributes.expression",  equalTo("{{player.name}}"))
                .body("data.attributes.tableSource",  nullValue())
                .body("data.relationships.table.data.id", equalTo("playerStats"));
    }

    @Test
    public void dimensionValuesOnReferenceTest() {

        given()
                .accept("application/vnd.api+json")
                .get("/table/playerStats/dimensions/playerStats.countryIsoCode")
                .then()
                .body("data.attributes.values", hasItems("US", "HK"))
                .body("data.attributes.valueSourceType", equalTo("ENUM"))
                .body("data.attributes.tableSource", nullValue())
                .body("data.attributes.columnType", equalTo("FORMULA"))
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void dimensionValuesOnFieldTest() {

        given()
                .accept("application/vnd.api+json")
                .get("/table/playerStats/dimensions/playerStats.overallRating")
                .then()
                .body("data.attributes.values", hasItems("GOOD", "OK", "TERRIBLE"))
                .body("data.attributes.valueSourceType", equalTo("ENUM"))
                .body("data.attributes.tableSource", nullValue())
                .body("data.attributes.columnType", equalTo("FIELD"))
                .body("data.attributes.cardinality", equalTo("MEDIUM"))
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void dimensionTagsTest() {

        given()
                .accept("application/vnd.api+json")
                .get("/table/playerStats/dimensions/playerStats.overallRating")
                .then()
                .body("data.attributes.tags", hasItems("PUBLIC"))
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void dimensionTableSourceOverrideTest() {

        given()
                .accept("application/vnd.api+json")
                .get("/table/playerStats/dimensions/playerStats.countryNickName")
                .then()
                .body("data.attributes.valueSourceType", equalTo("TABLE"))
                .body("data.attributes.columnType", equalTo("FORMULA"))
                .body("data.attributes.tableSource",  equalTo("subcountry.nickName"))
                .body("data.attributes.expression",  equalTo("{{country.nickName}}"))
                .body("data.attributes.values", equalTo(Collections.emptyList()))
                .body("data.attributes.cardinality", nullValue())
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void timeDimensionMetaDataTest() {

        given()
                .accept("application/vnd.api+json")
                .get("/table/playerStats/timeDimensions/playerStats.recordedDate?include=supportedGrain")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.name", equalTo("recordedDate"))
                .body("data.attributes.valueType",  equalTo("TIME"))
                .body("data.attributes.columnType",  equalTo("FORMULA"))
                .body("data.attributes.expression",  equalTo("{{recordedDate}}"))
                .body("data.relationships.table.data.id", equalTo("playerStats"))
                .body("data.relationships.supportedGrain.data.id", hasItem("playerStats.recordedDate.day"))
                .body("included.id", hasItem("playerStats.recordedDate.day"))
                .body("included.attributes.grain", hasItem("DAY"))
                .body("included.attributes.expression",
                        hasItem("PARSEDATETIME(FORMATDATETIME({{}}, 'yyyy-MM-dd'), 'yyyy-MM-dd')"));
    }

    @Test
    public void metricMetaDataTest() {

        given()
                .accept("application/vnd.api+json")
                .get("/table/playerStats/metrics/playerStats.lowScore?include=metricFunction")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.name", equalTo("lowScore"))
                .body("data.attributes.valueType",  equalTo("INTEGER"))
                .body("data.attributes.columnType",  equalTo("FORMULA"))
                .body("data.attributes.expression",  equalTo("MIN({{lowScore}})"))
                .body("data.attributes.category",  equalTo("Score Category"))
                .body("data.attributes.description",  equalTo("very low score"))
                .body("data.attributes.tags",  hasItems("PRIVATE"))
                .body("data.relationships.table.data.id", equalTo("playerStats"))
                .body("data.relationships.metricFunction.data.id", equalTo("playerStats.lowScore[lowScore]"))
                .body("included.id", hasItem("playerStats.lowScore[lowScore]"))
                .body("included.attributes.description", hasItem("very low score"));

        given()
                .accept("application/vnd.api+json")
                .get("/table/videoGame/metrics/videoGame.timeSpentPerSession")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.name", equalTo("timeSpentPerSession"))
                .body("data.attributes.valueType",  equalTo("DECIMAL"))
                .body("data.attributes.columnType",  equalTo("FORMULA"))
                .body("data.attributes.expression",  equalTo("({{timeSpent}} / (CASE WHEN SUM({{game_rounds}}) = 0 THEN 1 ELSE {{sessions}} END))"))
                .body("data.relationships.table.data.id", equalTo("videoGame"));

    }

    @Test
    public void versioningTest() {

        given()
                .accept("application/vnd.api+json")
                .get("/table")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data", hasSize(greaterThan(30)));

        given()
                .header("ApiVersion", "")
                .accept("application/vnd.api+json")
                .get("/table")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data", hasSize(greaterThan(30)));

        given()
                .header("ApiVersion", "1.0")
                .accept("application/vnd.api+json")
                .get("/table")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data", hasSize(1))
                .body("data.attributes.name", hasItem("book"));

        given()
                .header("ApiVersion", "2.0")
                .accept("application/vnd.api+json")
                .get("/table")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("errors.detail", hasItem("API version 2.0 not found"));
    }

    @Test
    public void dynamicConfigCardinalityMetaDataTest() {

        given()
                .accept("application/vnd.api+json")
                .get("/table/orderDetails/dimensions/orderDetails.customerRegion")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.name", equalTo("customerRegion"))
                .body("data.attributes.cardinality",  equalTo("SMALL"))
                .body("data.attributes.expression",  equalTo("{{customer.customerRegion}}"));

        given()
                .accept("application/vnd.api+json")
                .get("/table/orderDetails/dimensions/orderDetails.customerRegionRegion")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.name", equalTo("customerRegionRegion"))
                .body("data.attributes.cardinality",  nullValue())
                .body("data.attributes.expression",  equalTo("{{customer.region.region}}"));
    }
}
