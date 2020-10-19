/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.yahoo.elide.contrib.dynamicconfighelpers.compile.ConnectionDetails;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.dialect.ParseException;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.core.sort.SortingImpl;
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import com.yahoo.elide.datastores.aggregation.example.Continent;
import com.yahoo.elide.datastores.aggregation.example.Country;
import com.yahoo.elide.datastores.aggregation.example.CountryView;
import com.yahoo.elide.datastores.aggregation.example.CountryViewNested;
import com.yahoo.elide.datastores.aggregation.example.Player;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.example.PlayerStatsView;
import com.yahoo.elide.datastores.aggregation.example.PlayerStatsWithView;
import com.yahoo.elide.datastores.aggregation.example.SubCountry;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.query.ImmutablePagination;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialect;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;
import com.yahoo.elide.datastores.aggregation.timegrains.DateTime;
import com.yahoo.elide.datastores.aggregation.timegrains.MonthYear;
import com.yahoo.elide.datastores.aggregation.timegrains.SimpleDate;
import com.yahoo.elide.datastores.aggregation.timegrains.WeekDateISO;
import com.yahoo.elide.datastores.aggregation.timegrains.Year;
import com.yahoo.elide.datastores.aggregation.timegrains.YearMonth;
import com.yahoo.elide.datastores.aggregation.timegrains.serde.DateTimeSerde;
import com.yahoo.elide.datastores.aggregation.timegrains.serde.MonthYearSerde;
import com.yahoo.elide.datastores.aggregation.timegrains.serde.SimpleDateSerde;
import com.yahoo.elide.datastores.aggregation.timegrains.serde.WeekDateSerde;
import com.yahoo.elide.datastores.aggregation.timegrains.serde.YearMonthSerde;
import com.yahoo.elide.datastores.aggregation.timegrains.serde.YearSerde;
import com.yahoo.elide.request.Sorting;
import com.yahoo.elide.utils.ClassScanner;
import com.yahoo.elide.utils.coerce.CoerceUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Provider;
import javax.sql.DataSource;

public abstract class SQLUnitTest {

    protected static SQLTable playerStatsTable;
    protected static EntityDictionary dictionary;
    protected static RSQLFilterDialect filterParser;
    protected static MetaDataStore metaDataStore;

    protected static final Country HONG_KONG = new Country();
    protected static final Country USA = new Country();
    protected static final Continent ASIA = new Continent();
    protected static final Continent NA = new Continent();

    protected static QueryEngine engine;

    protected QueryEngine.Transaction transaction;

    // Standard set of test queries used in dialect tests
    protected enum TestQuery {
        WHERE_METRICS_ONLY (() -> {
            return Query.builder()
                    .source(playerStatsTable)
                    .metricProjection(playerStatsTable.getMetricProjection("lowScore"))
                    .whereFilter(new FilterPredicate(
                            new Path(PlayerStats.class, dictionary, "lowScore"),
                            Operator.GT,
                            Arrays.asList(9000)))
                    .build();
        }),
        WHERE_DIMS_ONLY (() -> {
            return Query.builder()
                    .source(playerStatsTable)
                    .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                    .whereFilter(new FilterPredicate(
                            new Path(PlayerStats.class, dictionary, "overallRating"),
                            Operator.NOTNULL,
                            new ArrayList<Object>()))
                    .build();
        }),
        WHERE_METRICS_AND_DIMS (() -> {
            FilterPredicate ratingFilter = new FilterPredicate(
                    new Path(PlayerStats.class, dictionary, "overallRating"),
                    Operator.NOTNULL, new ArrayList<Object>());
            FilterPredicate highScoreFilter = new FilterPredicate(
                    new Path(PlayerStats.class, dictionary, "highScore"),
                    Operator.GT,
                    Arrays.asList(9000));
            return Query.builder()
                    .source(playerStatsTable)
                    .metricProjection(playerStatsTable.getMetricProjection("highScore"))
                    .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                    .whereFilter(new AndFilterExpression(ratingFilter, highScoreFilter))
                    .build();
        }),
        WHERE_METRICS_OR_DIMS (() -> {
            FilterPredicate ratingFilter = new FilterPredicate(
                    new Path(PlayerStats.class, dictionary, "overallRating"),
                    Operator.NOTNULL, new ArrayList<Object>());
            FilterPredicate highScoreFilter = new FilterPredicate(
                    new Path(PlayerStats.class, dictionary, "highScore"),
                    Operator.GT,
                    Arrays.asList(9000));
            return Query.builder()
                    .source(playerStatsTable)
                    .metricProjection(playerStatsTable.getMetricProjection("highScore"))
                    .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                    .whereFilter(new OrFilterExpression(ratingFilter, highScoreFilter))
                    .build();
        }),
        WHERE_METRICS_AGGREGATION (() -> {
            return Query.builder()
                    .source(playerStatsTable)
                    .metricProjection(playerStatsTable.getMetricProjection("highScore"))
                    .metricProjection(playerStatsTable.getMetricProjection("lowScore"))
                    .whereFilter(new FilterPredicate(
                            new Path(PlayerStats.class, dictionary, "highScore"),
                            Operator.GT,
                            Arrays.asList(9000)))
                    .build();
        }),
        HAVING_METRICS_ONLY (() -> {
            return Query.builder()
                    .source(playerStatsTable)
                    .metricProjection(playerStatsTable.getMetricProjection("lowScore"))
                    .havingFilter(new FilterPredicate(
                            new Path(PlayerStats.class, dictionary, "lowScore"),
                            Operator.GT,
                            Arrays.asList(9000)))
                    .build();
        }),
        HAVING_DIMS_ONLY (() -> {
            return Query.builder()
                    .source(playerStatsTable)
                    .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                    .havingFilter(new FilterPredicate(
                            new Path(PlayerStats.class, dictionary, "overallRating"),
                            Operator.NOTNULL,
                            new ArrayList<Object>()))
                    .build();
        }),
        HAVING_METRICS_AND_DIMS (() -> {
            FilterPredicate ratingFilter = new FilterPredicate(
                    new Path(PlayerStats.class, dictionary, "overallRating"),
                    Operator.NOTNULL, new ArrayList<Object>());
            FilterPredicate highScoreFilter = new FilterPredicate(
                    new Path(PlayerStats.class, dictionary, "highScore"),
                    Operator.GT,
                    Arrays.asList(9000));
            return Query.builder()
                    .source(playerStatsTable)
                    .metricProjection(playerStatsTable.getMetricProjection("highScore"))
                    .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                    .havingFilter(new AndFilterExpression(ratingFilter, highScoreFilter))
                    .build();
        }),
        HAVING_METRICS_OR_DIMS (() -> {
            FilterPredicate ratingFilter = new FilterPredicate(
                    new Path(PlayerStats.class, dictionary, "overallRating"),
                    Operator.NOTNULL, new ArrayList<Object>());
            FilterPredicate highScoreFilter = new FilterPredicate(
                    new Path(PlayerStats.class, dictionary, "highScore"),
                    Operator.GT,
                    Arrays.asList(9000));
            return Query.builder()
                    .source(playerStatsTable)
                    .metricProjection(playerStatsTable.getMetricProjection("highScore"))
                    .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                    .havingFilter(new OrFilterExpression(ratingFilter, highScoreFilter))
                    .build();
        }),
        PAGINATION_TOTAL (() -> {
            return Query.builder()
                    .source(playerStatsTable)
                    .metricProjection(playerStatsTable.getMetricProjection("lowScore"))
                    .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                    .timeDimensionProjection(playerStatsTable.getTimeDimensionProjection("recordedDate"))
                    .pagination(new ImmutablePagination(0, 1, false, true))
                    .build();
        }),
        SORT_METRIC_ASC (() -> {
            Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
            sortMap.put("lowScore", Sorting.SortOrder.asc);
            return Query.builder()
                    .source(playerStatsTable)
                    .metricProjection(playerStatsTable.getMetricProjection("lowScore"))
                    .sorting(new SortingImpl(sortMap, PlayerStats.class, dictionary))
                    .build();
        }),
        SORT_METRIC_DESC (() -> {
            Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
            sortMap.put("lowScore", Sorting.SortOrder.desc);
            return Query.builder()
                    .source(playerStatsTable)
                    .metricProjection(playerStatsTable.getMetricProjection("lowScore"))
                    .sorting(new SortingImpl(sortMap, PlayerStats.class, dictionary))
                    .build();
        }),
        SORT_DIM_DESC (() -> {
            Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
            sortMap.put("overallRating", Sorting.SortOrder.desc);
            return Query.builder()
                    .source(playerStatsTable)
                    .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                    .sorting(new SortingImpl(sortMap, PlayerStats.class, dictionary))
                    .build();
        }),
        SORT_METRIC_AND_DIM_DESC (() -> {
            Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
            sortMap.put("highScore", Sorting.SortOrder.desc);
            sortMap.put("overallRating", Sorting.SortOrder.desc);
            return Query.builder()
                    .source(playerStatsTable)
                    .metricProjection(playerStatsTable.getMetricProjection("highScore"))
                    .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                    .sorting(new SortingImpl(sortMap, PlayerStats.class, dictionary))
                    .build();
        }),
        SUBQUERY (() -> {
            SQLTable playerStatsViewTable = (SQLTable) engine.getTable("playerStatsView");
            return Query.builder()
                    .source(playerStatsViewTable)
                    .metricProjection(playerStatsViewTable.getMetricProjection("highScore"))
                    .build();
        }),
        ORDER_BY_DIMENSION_NOT_IN_SELECT (() -> {
            Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
            sortMap.put("overallRating", Sorting.SortOrder.desc);
            return Query.builder()
                    .source(playerStatsTable)
                    .metricProjection(playerStatsTable.getMetricProjection("highScore"))
                    .sorting(new SortingImpl(sortMap, PlayerStats.class, dictionary))
                    .build();
        }),
        COMPLICATED (() -> {
            // Sorting
            Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
            sortMap.put("lowScore", Sorting.SortOrder.desc);
            // WHERE filter
            FilterPredicate predicate = new FilterPredicate(
                    new Path(PlayerStats.class, dictionary, "lowScore"),
                    Operator.GT,
                    Arrays.asList(9000));
            return Query.builder()
                    .source(playerStatsTable)
                    .metricProjection(playerStatsTable.getMetricProjection("highScore"))
                    .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                    .timeDimensionProjection(playerStatsTable.getTimeDimensionProjection("recordedDate"))
                    .pagination(new ImmutablePagination(10, 5, false, true))
                    .sorting(new SortingImpl(sortMap, PlayerStats.class, dictionary))
                    .whereFilter(predicate)
                    // force a join to look up countryIsoCode
                    .havingFilter(parseFilterExpression("countryIsoCode==USA",
                            PlayerStats.class, false))
                    .build();
        }),
        NESTED_METRIC_QUERY (() -> {
            // Sorting
            return Query.builder()
                    .source(playerStatsTable)
                    .metricProjection(playerStatsTable.getMetricProjection("dailyAverageScorePerPeriod"))
                    .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                    .timeDimensionProjection(playerStatsTable.getTimeDimensionProjection("recordedMonth"))
                    .build();
        }),
        NESTED_METRIC_WITH_HAVING_QUERY (() -> {
            // Sorting
            return Query.builder()
                    .source(playerStatsTable)
                    .metricProjection(playerStatsTable.getMetricProjection("dailyAverageScorePerPeriod"))
                    .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                    .timeDimensionProjection(playerStatsTable.getTimeDimensionProjection("recordedMonth"))
                    .havingFilter(new FilterPredicate(
                            new Path(PlayerStats.class, dictionary, "dailyAverageScorePerPeriod"),
                            Operator.GT,
                            Arrays.asList(100)))
                    .build();
        }),
        NESTED_METRIC_WITH_WHERE_QUERY (() -> {
            // Sorting
            return Query.builder()
                    .source(playerStatsTable)
                    .metricProjection(playerStatsTable.getMetricProjection("dailyAverageScorePerPeriod"))
                    .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                    .timeDimensionProjection(playerStatsTable.getTimeDimensionProjection("recordedMonth"))
                    .whereFilter(parseFilterExpression("countryIsoCode==USA",
                            PlayerStats.class, false))
                    .build();
        }),
        NESTED_METRIC_WITH_PAGINATION_QUERY (() -> {
            // Sorting
            return Query.builder()
                    .source(playerStatsTable)
                    .metricProjection(playerStatsTable.getMetricProjection("dailyAverageScorePerPeriod"))
                    .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                    .timeDimensionProjection(playerStatsTable.getTimeDimensionProjection("recordedMonth"))
                    .pagination(new ImmutablePagination(0, 1, false, true))
                    .build();
        }),
        NESTED_METRIC_WITH_SORTING_QUERY (() -> {
            Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
            sortMap.put("overallRating", Sorting.SortOrder.desc);
            sortMap.put("dailyAverageScorePerPeriod", Sorting.SortOrder.desc);
            // Sorting
            return Query.builder()
                    .source(playerStatsTable)
                    .metricProjection(playerStatsTable.getMetricProjection("dailyAverageScorePerPeriod"))
                    .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                    .timeDimensionProjection(playerStatsTable.getTimeDimensionProjection("recordedMonth"))
                    .sorting(new SortingImpl(sortMap, PlayerStats.class, dictionary))
                    .build();
        });

        private Provider<Query> queryProvider;

        TestQuery(Provider<Query> p) {
            queryProvider = p;
        }

        public Query getQuery() {
            return queryProvider.get();
        }
    }

    protected Pattern repeatedWhitespacePattern = Pattern.compile("\\s\\s*");

    public static void init(String sqlDialect) {

        HikariConfig config = new HikariConfig(File.separator + "jpah2db.properties");
        DataSource dataSource = new HikariDataSource(config);

        try (Connection h2Conn = dataSource.getConnection()) {
            h2Conn.createStatement().execute("RUNSCRIPT FROM 'classpath:prepare_tables.sql'");
        } catch (SQLException e) {
            ((HikariDataSource) dataSource).close();
            throw new IllegalStateException(e);
        }

        metaDataStore = new MetaDataStore(ClassScanner.getAllClasses("com.yahoo.elide.datastores.aggregation.example"));

        dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(PlayerStatsWithView.class);
        dictionary.bindEntity(PlayerStatsView.class);
        dictionary.bindEntity(PlayerStats.class);
        dictionary.bindEntity(Country.class);
        dictionary.bindEntity(SubCountry.class);
        dictionary.bindEntity(Player.class);
        dictionary.bindEntity(CountryView.class);
        dictionary.bindEntity(CountryViewNested.class);
        dictionary.bindEntity(Continent.class);
        filterParser = new RSQLFilterDialect(dictionary);

        //Manually register the serdes because we are not running a complete Elide service.
        CoerceUtil.register(YearMonth.class, new YearMonthSerde());
        CoerceUtil.register(SimpleDate.class, new SimpleDateSerde());
        CoerceUtil.register(DateTime.class, new DateTimeSerde());
        CoerceUtil.register(MonthYear.class, new MonthYearSerde());
        CoerceUtil.register(Year.class, new YearSerde());
        CoerceUtil.register(WeekDateISO.class, new WeekDateSerde());

        metaDataStore.populateEntityDictionary(dictionary);

        engine = new SQLQueryEngine(metaDataStore, new ConnectionDetails(dataSource, sqlDialect));

        playerStatsTable = (SQLTable) engine.getTable("playerStats");

    }

    public static void init() {
        init(SQLDialectFactory.getDefaultDialect());
    }

    public static void init(SQLDialect sqlDialect) {
        init(sqlDialect.getClass().getName());
    }

    @BeforeEach
    public void begin() {
        transaction = engine.beginTransaction();
    }

    @AfterEach
    public void end() {
        transaction.close();
    }

    public static TimeDimensionProjection toProjection(TimeDimension dimension, TimeGrain grain) {
        return engine.constructTimeDimensionProjection(
                dimension,
                dimension.getName(),
                Collections.emptyMap());
    }

    protected static List<Object> toList(Iterable<Object> data) {
        return StreamSupport.stream(data.spliterator(), false)
                .collect(Collectors.toList());
    }

    /*
     * Automatically convert a single expected string into a List with one element.
     */
    protected void compareQueryLists(String expected, List<String> actual) {
        compareQueryLists(Arrays.asList(expected), actual);
    }

    /*
     * Helper for comparing lists of queries.
     */
    protected void compareQueryLists(List<String> expected, List<String> actual) {
        if (expected == null && actual == null) {
            return;
        } else if (expected == null) {
            fail("Expected a null query List, but actual was non-null");
        } else if (actual == null) {
            fail("Expected a non-null query List, but actual was null");
        }

        assertEquals(expected.size(), actual.size(), "Query List sizes do not match");


        for (int i = 0; i < expected.size(); i++) {
            String actualMadeStatic = replaceDynamicAliases(actual.get(i).trim());
            assertEquals(combineWhitespace(expected.get(i).trim()), combineWhitespace(actualMadeStatic));
        }
    }

    /*
     * Helper to remove repeated whitespace chars before comparing queries.
     */
    protected String combineWhitespace(String input) {
        return repeatedWhitespacePattern.matcher(input).replaceAll(" ");
    }

    /*
     * Helper that wraps parseFilterExpression and converts ParseException to IllegalStateException.
     * Because this is for unit testing, the only time a ParseException should occur
     * is when a test is incorrectly configured.
     */
    private static FilterExpression parseFilterExpression(String expressionText, Class<?> entityType,
                                                          boolean allowNestedToManyAssociations) {
        try {
            return filterParser.parseFilterExpression(expressionText, entityType, allowNestedToManyAssociations);
        } catch (ParseException pe) {
            throw new IllegalStateException(pe);
        }
    }

    public static String replaceDynamicAliases(String queryText) {
        //Replaces :dailyAverage_12345_0 with :XXX
        String replaced = queryText.replaceAll(":[a-zA-Z0-9_]+", ":XXX");

        //Replaces Foo_12345.bar with Foo_XXX.bar
        replaced = replaced.replaceAll("_\\d+\\.", "_XXX\\.");

        //Replaces Foo_12345 with Foo_XXX
        replaced = replaced.replaceAll("_\\d+\\s+", "_XXX ");
        return replaced.replaceAll("_\\d+", "_XXX");
    }
}
