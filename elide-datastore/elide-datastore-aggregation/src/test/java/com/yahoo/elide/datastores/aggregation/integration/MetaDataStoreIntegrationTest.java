/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.integration;

import static com.yahoo.elide.datastores.aggregation.integration.AggregationDataStoreIntegrationTest.VALIDATOR;
import static com.yahoo.elide.datastores.aggregation.integration.AggregationDataStoreIntegrationTest.getDBPasswordExtractor;
import static com.yahoo.elide.datastores.aggregation.integration.AggregationDataStoreIntegrationTest.getDataSource;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.core.exceptions.HttpStatus;
import com.yahoo.elide.datastores.aggregation.framework.AggregationDataStoreTestHarness;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.ConnectionDetails;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialect;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
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
        VALIDATOR.getElideSQLDBConfig().getDbconfigs().forEach(dbConfig -> {
            connectionDetailsMap.put(dbConfig.getName(),
                            new ConnectionDetails(getDataSource(dbConfig, getDBPasswordExtractor()),
                                            SQLDialectFactory.getDialect(dbConfig.getDialect())));
        });

        return new AggregationDataStoreTestHarness(emf, defaultConnectionDetails, connectionDetailsMap, VALIDATOR);
    }

    @Test
    public void tableWithRequiredFilter() {
        given()
                .accept("application/vnd.api+json")
                .get("/table/playerStatsFiltered")
                .then()
                .body("data.attributes.requiredFilter", equalTo("recordedDate>={{start}};recordedDate<{{end}}"))
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void testStaticTableRelationshipsAreExcluded() {
        given()
                .accept("application/vnd.api+json")
                .get("/table/book/dimensions")
                .then()
                .body("data.id", containsInAnyOrder("book.language", "book.id",
                        "book.chapterCount", "book.publishDate", "book.editorName", "book.title", "book.genre"))
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void testStaticComplexAttributesAreExcluded() {
        given()
                .accept("application/vnd.api+json")
                .get("/table/embedded/dimensions")
                .then()
                .body("data.id", containsInAnyOrder("embedded.id"))
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void tableMetaDataTest() {

        given()
               .accept("application/vnd.api+json")
               .get("/table/planet")
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("data.attributes.isFact", equalTo(false)) //FromTable, TableMeta Present, isFact false
               .body("data.attributes.friendlyName", equalTo("planet"))
               .body("data.relationships.columns.data.id", containsInAnyOrder("planet.id", "planet.name"));
        given()
               .accept("application/vnd.api+json")
               .get("/table/continent")
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("data.attributes.isFact", equalTo(false)) //TableMeta Present, isFact false
               .body("data.attributes.friendlyName", equalTo("continent"))
               .body("data.relationships.columns.data.id", containsInAnyOrder("continent.id", "continent.name"));
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
                .body("data.attributes.isFact", equalTo(true)) //TableMeta Present, isFact default true
                .body("data.attributes.cardinality", equalTo("UNKNOWN"))
                .body("data.relationships.columns.data.id", containsInAnyOrder("country.name",
                        "country.inUsa", "country.unSeats", "country.nickName", "country.isoCode"));
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
                .body("data.attributes.tags", containsInAnyOrder("Statistics", "Game"))
                .body(
                        "data.relationships.dimensions.data.id",
                        containsInAnyOrder(
                                "playerStats.playerName",
                                "playerStats.playerRank",
                                "playerStats.playerLevel",
                                "playerStats.countryIsInUsa",
                                "playerStats.countryUnSeats",
                                "playerStats.countryNickName",
                                "playerStats.player2Name",
                                "playerStats.countryIsoCode",
                                "playerStats.subCountryIsoCode",
                                "playerStats.overallRating"))
                .body("data.relationships.metrics.data.id", containsInAnyOrder("playerStats.lowScore",
                        "playerStats.highScore", "playerStats.dailyAverageScorePerPeriod"))
                .body("data.relationships.timeDimensions.data.id", containsInAnyOrder("playerStats.recordedDate",
                        "playerStats.updatedDate"));
    }

    @Test
    public void dimensionMetaDataTest() {

        given()
                .accept("application/vnd.api+json")
                .get("/table/playerStats/dimensions/playerStats.playerName")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.name", equalTo("playerName"))
                .body("data.attributes.friendlyName", equalTo("playerName"))
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
                .body("data.attributes.values", containsInAnyOrder("US", "HK"))
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
                .body("data.attributes.values", containsInAnyOrder("GOOD", "OK", "TERRIBLE"))
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
                .body("data.attributes.tags", containsInAnyOrder("PUBLIC"))
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
                .body("data.attributes.cardinality", equalTo("UNKNOWN"))
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void timeDimensionMetaDataTest() {

        given()
                .accept("application/vnd.api+json")
                .get("/table/playerStats/timeDimensions/playerStats.recordedDate?include=supportedGrains")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.name", equalTo("recordedDate"))
                .body("data.attributes.friendlyName", equalTo("recordedDate"))
                .body("data.attributes.valueType",  equalTo("TIME"))
                .body("data.attributes.columnType",  equalTo("FORMULA"))
                .body("data.attributes.expression",  equalTo("{{recordedDate}}"))
                .body("data.relationships.table.data.id", equalTo("playerStats"))
                .body("data.relationships.supportedGrains.data.id", containsInAnyOrder("playerStats.recordedDate.day", "playerStats.recordedDate.month"))
                .body("included.id", containsInAnyOrder("playerStats.recordedDate.day", "playerStats.recordedDate.month"))
                .body("included.attributes.grain", containsInAnyOrder("DAY", "MONTH"))
                .body("included.attributes.expression",
                        containsInAnyOrder(
                                "PARSEDATETIME(FORMATDATETIME({{}}, 'yyyy-MM-dd'), 'yyyy-MM-dd')",
                                        "PARSEDATETIME(FORMATDATETIME({{}}, 'yyyy-MM'), 'yyyy-MM')"
                        ));
    }

    @Test
    public void metricMetaDataTest() {

        given()
                .accept("application/vnd.api+json")
                .get("/table/playerStats/metrics/playerStats.lowScore?include=metricFunction")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.name", equalTo("lowScore"))
                .body("data.attributes.friendlyName", equalTo("lowScore"))
                .body("data.attributes.valueType",  equalTo("INTEGER"))
                .body("data.attributes.columnType",  equalTo("FORMULA"))
                .body("data.attributes.expression",  equalTo("MIN({{lowScore}})"))
                .body("data.attributes.category",  equalTo("Score Category"))
                .body("data.attributes.description",  equalTo("very low score"))
                .body("data.attributes.tags",  containsInAnyOrder("PRIVATE"))
                .body("data.relationships.table.data.id", equalTo("playerStats"))
                .body("data.relationships.metricFunction.data.id", equalTo("playerStats.lowScore[lowScore]"))
                .body("included.id", containsInAnyOrder("playerStats.lowScore[lowScore]"))
                .body("included.attributes.description", containsInAnyOrder("very low score"));

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
                .body("data.attributes.name", containsInAnyOrder("book"));

        given()
                .header("ApiVersion", "2.0")
                .accept("application/vnd.api+json")
                .get("/table")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("errors.detail", containsInAnyOrder("API version 2.0 not found"));
    }

    @Test
    public void dynamicConfigMetaDataTest() {

        given()
                .accept("application/vnd.api+json")
                .get("/table/orderDetails/dimensions/orderDetails.customerRegion")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.name", equalTo("customerRegion"))
                .body("data.attributes.cardinality", equalTo("SMALL"))
                .body("data.attributes.expression", equalTo("{{customer.customerRegion}}"))
                .body("data.attributes.tableSource", nullValue())
                .body("data.attributes.valueSourceType", equalTo("NONE"));

        given()
                .accept("application/vnd.api+json")
                .get("/table/orderDetails/dimensions/orderDetails.customerRegionRegion")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.name", equalTo("customerRegionRegion"))
                .body("data.attributes.cardinality", equalTo("UNKNOWN"))
                .body("data.attributes.expression", equalTo("{{customer.region.region}}"))
                .body("data.attributes.tableSource", equalTo("regionDetails.region"))
                .body("data.attributes.valueSourceType", equalTo("TABLE"));
    }
}
