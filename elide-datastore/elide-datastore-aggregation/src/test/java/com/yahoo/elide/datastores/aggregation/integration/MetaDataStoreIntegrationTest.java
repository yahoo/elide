/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.integration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.nullValue;

import com.yahoo.elide.contrib.dynamicconfighelpers.compile.ConnectionDetails;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.datastores.aggregation.framework.AggregationDataStoreTestHarness;
import com.yahoo.elide.datastores.aggregation.metadata.models.TableId;
import com.yahoo.elide.datastores.aggregation.metadata.models.TableIdSerde;
import com.yahoo.elide.initialization.IntegrationTest;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Collections;
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

        return new AggregationDataStoreTestHarness(emf, defaultConnectionDetails);
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

        TableIdSerde serde = new TableIdSerde();
        String countryId = serde.serialize(new TableId("country", "", ""));
        String playerStatsId = serde.serialize(new TableId("playerStats", "", ""));
        given()
                .accept("application/vnd.api+json")
                .get("/table/" + countryId)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.cardinality", equalTo("SMALL"))
                .body("data.relationships.columns.data.id", hasItems("country.id", "country.name", "country.isoCode"));
        given()
                .accept("application/vnd.api+json")
                .get("/table/" + playerStatsId)
                .then()
                .statusCode(HttpStatus.SC_OK)
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
                .get("/dimension/playerStats.playerName")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.name", equalTo("playerName"))
                .body("data.attributes.valueType",  equalTo("TEXT"))
                .body("data.attributes.columnType",  equalTo("REFERENCE"))
                .body("data.attributes.valueSourceType",  equalTo("TABLE"))
                .body("data.attributes.expression",  equalTo("player.name"))
                .body("data.attributes.tableSource",  equalTo("player.name"))
                .body("data.relationships.table.data.id", equalTo(getTableId("playerStats")));
    }

    @Test
    public void dimensionValuesOnReferenceTest() {

        given()
                .accept("application/vnd.api+json")
                .get("/dimension/playerStats.countryIsoCode")
                .then()
                .body("data.attributes.values", hasItems("US", "HK"))
                .body("data.attributes.valueSourceType", equalTo("ENUM"))
                .body("data.attributes.tableSource", nullValue())
                .body("data.attributes.columnType", equalTo("REFERENCE"))
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void dimensionValuesOnFieldTest() {

        given()
                .accept("application/vnd.api+json")
                .get("/dimension/playerStats.overallRating")
                .then()
                .body("data.attributes.values", hasItems("GOOD", "OK", "TERRIBLE"))
                .body("data.attributes.valueSourceType", equalTo("ENUM"))
                .body("data.attributes.tableSource", nullValue())
                .body("data.attributes.columnType", equalTo("FIELD"))
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void dimensionTagsTest() {

        given()
                .accept("application/vnd.api+json")
                .get("/dimension/playerStats.overallRating")
                .then()
                .body("data.attributes.tags", hasItems("PUBLIC"))
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void dimensionTableSourceOverrideTest() {

        given()
                .accept("application/vnd.api+json")
                .get("/dimension/playerStats.countryNickName")
                .then()
                .body("data.attributes.valueSourceType", equalTo("TABLE"))
                .body("data.attributes.columnType", equalTo("REFERENCE"))
                .body("data.attributes.tableSource",  equalTo("subcountry.nickName"))
                .body("data.attributes.expression",  equalTo("country.nickName"))
                .body("data.attributes.values", equalTo(Collections.emptyList()))
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void timeDimensionMetaDataTest() {

        given()
                .accept("application/vnd.api+json")
                .get("/timeDimension/playerStats.recordedDate?include=supportedGrain")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.name", equalTo("recordedDate"))
                .body("data.attributes.valueType",  equalTo("TIME"))
                .body("data.attributes.columnType",  equalTo("FIELD"))
                .body("data.attributes.expression",  equalTo("recordedDate"))
                .body("data.relationships.table.data.id", equalTo(getTableId("playerStats")))
                .body("data.relationships.supportedGrain.data.id", hasItem("playerStats.recordedDate.simpledate"))
                .body("included.id", hasItem("playerStats.recordedDate.simpledate"))
                .body("included.attributes.grain", hasItem("SIMPLEDATE"))
                .body("included.attributes.expression",
                        hasItem("PARSEDATETIME(FORMATDATETIME({{}}, 'yyyy-MM-dd'), 'yyyy-MM-dd')"));
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
                .body("data.attributes.tags",  hasItems("PRIVATE"))
                .body("data.relationships.table.data.id", equalTo(getTableId("playerStats")))
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
                .body("data.relationships.table.data.id", equalTo(getTableId("videoGame", "", "mycon")));

    }

    private String getTableId(String name, String version, String dbConnectionName) {
        return new TableId(name, version, dbConnectionName).toString();
    }

    private String getTableId(String name) {
        return getTableId(name, "", "");
    }
}
