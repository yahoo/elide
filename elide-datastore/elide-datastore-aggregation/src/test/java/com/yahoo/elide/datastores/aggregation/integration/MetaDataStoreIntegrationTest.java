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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.HttpStatus;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.filter.dialect.jsonapi.DefaultFilterDialect;
import com.yahoo.elide.core.filter.dialect.jsonapi.MultipleFilterDialect;
import com.yahoo.elide.core.security.checks.Check;
import com.yahoo.elide.core.security.checks.prefab.Role;
import com.yahoo.elide.datastores.aggregation.checks.OperatorCheck;
import com.yahoo.elide.datastores.aggregation.checks.VideoGameFilterCheck;
import com.yahoo.elide.datastores.aggregation.framework.NoCacheAggregationDataStoreTestHarness;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.ConnectionDetails;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialect;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.yahoo.elide.initialization.IntegrationTest;
import com.yahoo.elide.jsonapi.resources.JsonApiEndpoint;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import example.TestCheckMappings;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.io.File;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;
import javax.sql.DataSource;

public class MetaDataStoreIntegrationTest extends IntegrationTest {

    private static final class SecurityHjsonIntegrationTestResourceConfig extends ResourceConfig {

        @Inject
        public SecurityHjsonIntegrationTestResourceConfig() {
            register(new AbstractBinder() {
                @Override
                protected void configure() {
                    Map<String, Class<? extends Check>> map = new HashMap<>(TestCheckMappings.MAPPINGS);
                    map.put(OperatorCheck.OPERTOR_CHECK, OperatorCheck.class);
                    map.put(VideoGameFilterCheck.NAME_FILTER, VideoGameFilterCheck.class);
                    EntityDictionary dictionary = EntityDictionary.builder().checks(map).build();

                    VALIDATOR.getElideSecurityConfig().getRoles().forEach(role ->
                            dictionary.addRoleCheck(role, new Role.RoleMemberCheck(role))
                    );

                    DefaultFilterDialect defaultFilterStrategy = new DefaultFilterDialect(dictionary);
                    RSQLFilterDialect rsqlFilterStrategy = RSQLFilterDialect.builder().dictionary(dictionary).build();

                    MultipleFilterDialect multipleFilterStrategy = new MultipleFilterDialect(
                            Arrays.asList(rsqlFilterStrategy, defaultFilterStrategy),
                            Arrays.asList(rsqlFilterStrategy, defaultFilterStrategy)
                    );

                    Elide elide = new Elide(new ElideSettingsBuilder(IntegrationTest.getDataStore())
                            .withJoinFilterDialect(multipleFilterStrategy)
                            .withSubqueryFilterDialect(multipleFilterStrategy)
                            .withEntityDictionary(dictionary)
                            .withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", Calendar.getInstance().getTimeZone())
                            .build());

                    elide.doScans();
                    bind(elide).to(Elide.class).named("elide");
                }
            });
        }
    }

    public MetaDataStoreIntegrationTest() {
        super(SecurityHjsonIntegrationTestResourceConfig.class, JsonApiEndpoint.class.getPackage().getName());
    }

    @Override
    protected DataStoreTestHarness createHarness() {

        HikariConfig config = new HikariConfig(File.separator + "jpah2db.properties");
        DataSource defaultDataSource = new HikariDataSource(config);
        SQLDialect defaultDialect = SQLDialectFactory.getDefaultDialect();
        ConnectionDetails defaultConnectionDetails = new ConnectionDetails(defaultDataSource, defaultDialect);

        Properties prop = new Properties();
        prop.put("jakarta.persistence.jdbc.driver", config.getDriverClassName());
        prop.put("jakarta.persistence.jdbc.url", config.getJdbcUrl());
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

        return new NoCacheAggregationDataStoreTestHarness(emf, defaultConnectionDetails, connectionDetailsMap, VALIDATOR);
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
                .body("data.id", containsInAnyOrder("book.language", "book.id", "book.awards", "book.price",
                        "book.chapterCount", "book.publishDate", "book.editorName", "book.title", "book.genre"))
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void testStaticComplexAttributesAreExcluded() {
        given()
                .accept("application/vnd.api+json")
                .get("/table/embedded/dimensions")
                .then()
                .body("data.id", containsInAnyOrder("embedded.id", "embedded.segmentIds"))
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void namespaceTest() {
        given()
                .accept("application/vnd.api+json")
                .get("/namespace/SalesNamespace")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.name", equalTo("SalesNamespace"))
                .body("data.attributes.friendlyName", equalTo("Sales"))
                .body("data.relationships.tables.data.id", contains(
                        "SalesNamespace_orderDetails",
                        "SalesNamespace_orderDetails2",
                        "SalesNamespace_deliveryDetails"));
        given()
                .accept("application/vnd.api+json")
                .get("/namespace/default")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.name", equalTo("default"))
                .body("data.attributes.friendlyName", equalTo("DEFAULT")) // Overridden value
                .body("data.attributes.description", equalTo("Default Namespace")) // Overridden value
                .body("data.relationships.tables.data.id", hasItems("playerStats", "country", "planet"));
        given()
                .accept("application/vnd.api+json")
                .get("/namespace/NoDescriptionNamespace") //"default" namespace added by Agg Store
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.name", equalTo("NoDescriptionNamespace"))
                .body("data.attributes.friendlyName", equalTo("NoDescriptionNamespace")) // No FriendlyName provided, defaulted to name
                .body("data.attributes.description", equalTo("NoDescriptionNamespace")) // No description provided, defaulted to name
                .body("data.relationships.tables.data.id", empty());
    }

    @Test
    public void tableSourceTest() {
        given()
                .accept("application/vnd.api+json")
                .get("/table/playerStats/dimensions/playerStats.countryNickName/tableSource")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.relationships.suggestionColumns.data.id", containsInAnyOrder("subCountry.id", "subCountry.isoCode"))
                .body("data.relationships.valueSource.data.id", equalTo("subCountry.name"));
    }

    @Test
    public void tableTest() {
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
                .body("data.relationships.columns.data.id", containsInAnyOrder("country.id", "country.name",
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
                                "playerStats.overallRating",
                                "playerStats.placeType1",
                                "playerStats.placeType2"))
                .body("data.relationships.metrics.data.id", containsInAnyOrder("playerStats.id", "playerStats.lowScore",
                        "playerStats.highScore", "playerStats.dailyAverageScorePerPeriod"))
                .body("data.relationships.timeDimensions.data.id", containsInAnyOrder("playerStats.recordedDate",
                        "playerStats.updatedDate"));
        // Verify Table Arguments
        given()
                .accept("application/vnd.api+json")
                .get("/table/SalesNamespace_orderDetails?include=arguments")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.relationships.arguments.data.id",
                        containsInAnyOrder("SalesNamespace_orderDetails.denominator"));
    }

    @Test
    public void hiddenDimensionTest() {

        //Non Hidden
        given()
                .accept("application/vnd.api+json")
                .get("/table/SalesNamespace_orderDetails/dimensions/SalesNamespace_orderDetails.zipCode")
                .then()
                .statusCode(HttpStatus.SC_OK);

        //Hidden
        given()
                .accept("application/vnd.api+json")
                .get("/table/SalesNamespace_orderDetails/dimensions/SalesNamespace_orderDetails.zipCodeHidden")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND);

        //Hidden
        given()
                .accept("application/vnd.api+json")
                .get("/table/SalesNamespace_orderDetails/columns")
                .then()
                .body("data.id", not(contains("SalesNamespace_orderDetails.zipCodeHidden")))
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void hiddenTableTest() {

        //Non Hidden
        given()
                .accept("application/vnd.api+json")
                .get("/table/SalesNamespace_orderDetails")
                .then()
                .statusCode(HttpStatus.SC_OK);

        //Hidden
        given()
                .accept("application/vnd.api+json")
                .get("/table/SalesNamespace_performance")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND);
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
                .body("data.relationships.table.data.id", equalTo("playerStats"))
                .body("data.relationships.arguments.data", empty()); // No Arguments were set.
    }

    @Test
    public void dimensionValuesOnReferenceTest() {

        given()
                .accept("application/vnd.api+json")
                .get("/table/playerStats/dimensions/playerStats.countryIsoCode")
                .then()
                .body("data.attributes.values", containsInAnyOrder("USA", "HKG"))
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
                .body("data.attributes.values", containsInAnyOrder("Good", "OK", "Great", "Terrible"))
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
    public void dimensionIdTest() {

        given()
                .accept("application/vnd.api+json")
                .get("/table/book/dimensions/book.id")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void metricIdTest() {

        given()
                .accept("application/vnd.api+json")
                .get("/table/planet/metrics/planet.id")
                .then()
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
                .body("data.attributes.expression",  equalTo("{{country.nickName}}"))
                .body("data.attributes.values", equalTo(Collections.emptyList()))
                .body("data.attributes.cardinality", equalTo("UNKNOWN"))
                .body("data.relationships.tableSource.data.id",  equalTo("subCountry.name"))
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
                .body("data.attributes.expression",  equalTo("{{$recordedDate}}"))
                .body("data.relationships.arguments.data", empty()) // No Arguments were set.
                .body("data.relationships.table.data.id", equalTo("playerStats"))
                .body("data.relationships.supportedGrains.data.id", containsInAnyOrder("playerStats.recordedDate.day", "playerStats.recordedDate.month", "playerStats.recordedDate.quarter"))
                .body("included.id", containsInAnyOrder("playerStats.recordedDate.day", "playerStats.recordedDate.month", "playerStats.recordedDate.quarter"))
                .body("included.attributes.grain", containsInAnyOrder("DAY", "MONTH", "QUARTER"))
                .body("included.attributes.expression",
                        containsInAnyOrder(
                                "PARSEDATETIME(FORMATDATETIME({{$$column.expr}}, 'yyyy-MM-dd'), 'yyyy-MM-dd')",
                                "PARSEDATETIME(FORMATDATETIME({{$$column.expr}}, 'yyyy-MM-01'), 'yyyy-MM-dd')",
                                "PARSEDATETIME(CONCAT(FORMATDATETIME({{$$column.expr}}, 'yyyy-'), LPAD(3 * QUARTER({{$$column.expr}}) - 2, 2, '0'), '-01'), 'yyyy-MM-dd')"
                        ));
    }

    @Test
    public void metricMetaDataTest() {

        given()
                .accept("application/vnd.api+json")
                .get("/table/playerStats/metrics/playerStats.lowScore")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.name", equalTo("lowScore"))
                .body("data.attributes.friendlyName", equalTo("lowScore"))
                .body("data.attributes.valueType",  equalTo("INTEGER"))
                .body("data.attributes.columnType",  equalTo("FORMULA"))
                .body("data.attributes.expression",  equalTo("MIN({{$lowScore}})"))
                .body("data.attributes.category",  equalTo("Score Category"))
                .body("data.attributes.description",  equalTo("very low score"))
                .body("data.attributes.tags",  containsInAnyOrder("PRIVATE"))
                .body("data.relationships.arguments.data", empty()) // No Arguments were set.
                .body("data.relationships.table.data.id", equalTo("playerStats"));

        given()
                .accept("application/vnd.api+json")
                .get("/table/videoGame/metrics/videoGame.timeSpentPerSession")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.name", equalTo("timeSpentPerSession"))
                .body("data.attributes.valueType",  equalTo("DECIMAL"))
                .body("data.attributes.columnType",  equalTo("FORMULA"))
                .body("data.attributes.expression",  equalTo("({{timeSpent}} / (CASE WHEN SUM({{$game_rounds}}) = 0 THEN 1 ELSE {{sessions}} END))"))
                .body("data.relationships.arguments.data", empty()) // No Arguments were set.
                .body("data.relationships.table.data.id", equalTo("videoGame"));

        // Verify Metric Arguments
        given()
                .accept("application/vnd.api+json")
                .get("/table/SalesNamespace_orderDetails/metrics/SalesNamespace_orderDetails.orderTotal?include=arguments")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.name", equalTo("orderTotal"))
                .body("data.attributes.friendlyName", equalTo("orderTotal"))
                .body("data.relationships.arguments.data.id", containsInAnyOrder("SalesNamespace_orderDetails.orderTotal.precision"));

        given()
                .accept("application/vnd.api+json")
                .get("/table/SalesNamespace_orderDetails/metrics/SalesNamespace_orderDetails.id")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.name", equalTo("id"))
                .body("data.attributes.friendlyName", equalTo("Row Number"))
                .body("data.attributes.valueType",  equalTo("ID"))
                .body("data.attributes.valueSourceType",  equalTo("NONE"))
                .body("data.attributes.columnType",  equalTo("FIELD"))
                .body("data.attributes.description",  equalTo("Row number for each record returned by a query."))
                .body("data.attributes.expression",  equalTo("{{$id}}"))
                .body("data.attributes.category",  nullValue())
                .body("data.attributes.tags",  empty())
                .body("data.attributes.values",  empty())
                .body("data.relationships.arguments.data", empty()) // No Arguments were set.
                .body("data.relationships.table.data.id", equalTo("SalesNamespace_orderDetails"));
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
                .get("/table/SalesNamespace_orderDetails/dimensions/SalesNamespace_orderDetails.customerRegion")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.name", equalTo("customerRegion"))
                .body("data.attributes.cardinality", equalTo("SMALL"))
                .body("data.attributes.expression", equalTo("{{customer.customerRegion}}"))
                .body("data.attributes.tableSource", nullValue())
                .body("data.attributes.valueSourceType", equalTo("NONE"));

        given()
                .accept("application/vnd.api+json")
                .get("/table/SalesNamespace_orderDetails/dimensions/SalesNamespace_orderDetails.customerRegionRegion")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.name", equalTo("customerRegionRegion"))
                .body("data.attributes.cardinality", equalTo("UNKNOWN"))
                .body("data.attributes.expression", equalTo("{{customer.region.region}}"))
                .body("data.attributes.valueSourceType", equalTo("TABLE"))
                .body("data.relationships.tableSource.data.id", equalTo("regionDetails.region"));
    }

    @Test
    public void testEnumDimensionTypes() {

        given()
                .accept("application/vnd.api+json")
                .get("/table/SalesNamespace_orderDetails/dimensions/SalesNamespace_orderDetails.customerRegionType1")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.name", equalTo("customerRegionType1"))
                .body("data.attributes.valueType", equalTo("TEXT"));

        given()
                .accept("application/vnd.api+json")
                .get("/table/SalesNamespace_orderDetails/dimensions/SalesNamespace_orderDetails.customerRegionType2")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.name", equalTo("customerRegionType2"))
                .body("data.attributes.valueType", equalTo("TEXT"));

        given()
                .accept("application/vnd.api+json")
                .get("/table/playerStats/dimensions/playerStats.placeType1")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.name", equalTo("placeType1"))
                .body("data.attributes.valueType", equalTo("TEXT"));

        given()
                .accept("application/vnd.api+json")
                .get("/table/playerStats/dimensions/playerStats.placeType2")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.name", equalTo("placeType2"))
                .body("data.attributes.valueType", equalTo("TEXT"));
    }
}
