/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.framework;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static com.yahoo.elide.core.type.ClassType.STRING_TYPE;
import static com.yahoo.elide.core.utils.TypeHelper.getClassType;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.dialect.ParseException;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.core.request.Attribute;
import com.yahoo.elide.core.request.Sorting;
import com.yahoo.elide.core.sort.SortingImpl;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.core.utils.DefaultClassScanner;
import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import com.yahoo.elide.datastores.aggregation.DefaultQueryValidator;
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;
import com.yahoo.elide.datastores.aggregation.query.DefaultQueryPlanMerger;
import com.yahoo.elide.datastores.aggregation.query.DimensionProjection;
import com.yahoo.elide.datastores.aggregation.query.ImmutablePagination;
import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.query.Optimizer;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.ConnectionDetails;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialect;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;
import com.yahoo.elide.datastores.aggregation.timegrains.Day;
import com.yahoo.elide.datastores.aggregation.timegrains.Hour;
import com.yahoo.elide.datastores.aggregation.timegrains.ISOWeek;
import com.yahoo.elide.datastores.aggregation.timegrains.Minute;
import com.yahoo.elide.datastores.aggregation.timegrains.Month;
import com.yahoo.elide.datastores.aggregation.timegrains.Quarter;
import com.yahoo.elide.datastores.aggregation.timegrains.Second;
import com.yahoo.elide.datastores.aggregation.timegrains.Week;
import com.yahoo.elide.datastores.aggregation.timegrains.Year;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import example.GameRevenue;
import example.Player;
import example.PlayerStats;
import example.PlayerStatsView;
import example.PlayerStatsWithView;
import example.dimensions.Continent;
import example.dimensions.Country;
import example.dimensions.CountryView;
import example.dimensions.CountryViewNested;
import example.dimensions.SubCountry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Provider;
import javax.sql.DataSource;

public abstract class SQLUnitTest {

    protected static SQLTable playerStatsTable;
    protected static SQLTable playerStatsViewTable;
    protected static Map<String, Argument> playerStatsViewTableArgs;
    protected static EntityDictionary dictionary;
    protected static RSQLFilterDialect filterParser;
    protected static MetaDataStore metaDataStore;

    private static final DataSource DUMMY_DATASOURCE = new HikariDataSource();
    private static final String NEWLINE = System.lineSeparator();
    protected static SQLQueryEngine engine;

    protected QueryEngine.Transaction transaction;
    private static SQLTable videoGameTable;

    protected static Type<?> playerStatsType = ClassType.of(PlayerStats.class);
    protected static Type<?> playerStatsViewType = ClassType.of(PlayerStatsView.class);

    // Standard set of test queries used in dialect tests
    protected enum TestQuery {
        WHERE_DIMS_ONLY (() ->
            Query.builder()
                    .source(playerStatsTable)
                    .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                    .whereFilter(new FilterPredicate(
                            new Path(PlayerStats.class, dictionary, "overallRating"),
                            Operator.NOTNULL,
                            new ArrayList<>()))
                    .build()
        ),
        WHERE_AND (() -> {
            FilterPredicate ratingFilter = new FilterPredicate(
                    new Path(PlayerStats.class, dictionary, "overallRating"),
                    Operator.NOTNULL, new ArrayList<>());
            FilterPredicate highScoreFilter = new FilterPredicate(
                new Path(PlayerStats.class, dictionary, "countryIsoCode"),
                    Operator.IN,
                    Arrays.asList("USA"));
            return Query.builder()
                    .source(playerStatsTable)
                    .metricProjection(playerStatsTable.getMetricProjection("highScore"))
                    .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                    .whereFilter(new AndFilterExpression(ratingFilter, highScoreFilter))
                    .build();
        }),
        WHERE_OR (() -> {
            FilterPredicate ratingFilter = new FilterPredicate(
                    new Path(PlayerStats.class, dictionary, "overallRating"),
                    Operator.NOTNULL, new ArrayList<>());
            FilterPredicate highScoreFilter = new FilterPredicate(
                    new Path(PlayerStats.class, dictionary, "countryIsoCode"),
                    Operator.IN,
                    Arrays.asList("USA"));
            return Query.builder()
                    .source(playerStatsTable)
                    .metricProjection(playerStatsTable.getMetricProjection("highScore"))
                    .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                    .whereFilter(new OrFilterExpression(ratingFilter, highScoreFilter))
                    .build();
        }),
        WHERE_WITH_ARGUMENTS (() -> {
            Set<Argument> dayArgument = new HashSet<>();
            dayArgument.add(Argument.builder().name("grain").value(TimeGrain.DAY).build());

            Map<String, Argument> monthArgument = new HashMap<>();
            monthArgument.put("grain", Argument.builder().name("grain").value(TimeGrain.MONTH).build());

            FilterPredicate dayFilter = new FilterPredicate(
                    new Path(playerStatsType, dictionary, "recordedDate", "recordedDate", dayArgument),
                    Operator.NOTNULL,
                    new ArrayList<>());

            return Query.builder()
                    .source(playerStatsTable)
                    .metricProjection(playerStatsTable.getMetricProjection("highScore"))
                    .timeDimensionProjection(playerStatsTable.getTimeDimensionProjection("recordedDate", "recordedDate", monthArgument))
                    .whereFilter(dayFilter)
                    .build();
        }),
        HAVING_METRICS_ONLY (() ->
            Query.builder()
                    .source(playerStatsTable)
                    .metricProjection(playerStatsTable.getMetricProjection("lowScore"))
                    .havingFilter(new FilterPredicate(
                            new Path(PlayerStats.class, dictionary, "lowScore"),
                            Operator.GT,
                            Arrays.asList(9000)))
                    .build()
        ),
        HAVING_DIMS_ONLY (() ->
            Query.builder()
                    .source(playerStatsTable)
                    .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                    .havingFilter(new FilterPredicate(
                            new Path(PlayerStats.class, dictionary, "overallRating"),
                            Operator.NOTNULL,
                            new ArrayList<>()))
                    .build()
        ),
        HAVING_METRICS_AND_DIMS (() -> {
            FilterPredicate ratingFilter = new FilterPredicate(
                    new Path(PlayerStats.class, dictionary, "overallRating"),
                    Operator.NOTNULL, new ArrayList<>());
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
                    Operator.NOTNULL, new ArrayList<>());
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
        PAGINATION_TOTAL (() ->
            Query.builder()
                    .source(playerStatsTable)
                    .metricProjection(playerStatsTable.getMetricProjection("lowScore"))
                    .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                    .timeDimensionProjection(playerStatsTable.getTimeDimensionProjection("recordedDate"))
                    .pagination(new ImmutablePagination(0, 1, false, true))
                    .build()
        ),
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
            return Query.builder()
                    .source(playerStatsViewTable)
                    .metricProjection(playerStatsViewTable.getMetricProjection("highScore"))
                    .arguments(playerStatsViewTableArgs)
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
        PAGINATION_METRIC_ONLY (() ->
            Query.builder()
                    .source(playerStatsTable)
                    .metricProjection(playerStatsTable.getMetricProjection("lowScore"))
                    .pagination(new ImmutablePagination(10, 5, false, true))
                    .build()
        ),
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
                    .havingFilter(predicate)
                    // force a join to look up countryIsoCode
                    .whereFilter(parseFilterExpression("countryIsoCode==USA", playerStatsType, false))
                    .build();
        }),
        NESTED_METRIC_QUERY (() -> {
            Map<String, Argument> arguments = new HashMap<>();
            arguments.put("grain", Argument.builder().name("grain").value(TimeGrain.MONTH).build());
            // Sorting
            return Query.builder()
                    .source(playerStatsTable)
                    .metricProjection(playerStatsTable.getMetricProjection("dailyAverageScorePerPeriod"))
                    .metricProjection(playerStatsTable.getMetricProjection("highScore"))
                    .metricProjection(playerStatsTable.getMetricProjection("lowScore"))
                    .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                    .timeDimensionProjection(playerStatsTable.getTimeDimensionProjection("recordedDate", arguments))
                    .build();
        }),
        NESTED_METRIC_WITH_HAVING_QUERY (() -> {
            Map<String, Argument> arguments = new HashMap<>();
            arguments.put("grain", Argument.builder().name("grain").value(TimeGrain.MONTH).build());
            // Sorting
            return Query.builder()
                    .source(playerStatsTable)
                    .metricProjection(playerStatsTable.getMetricProjection("dailyAverageScorePerPeriod"))
                    .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                    .timeDimensionProjection(playerStatsTable.getTimeDimensionProjection("recordedDate", arguments))
                    .havingFilter(new FilterPredicate(
                            new Path(PlayerStats.class, dictionary, "dailyAverageScorePerPeriod"),
                            Operator.GT,
                            Arrays.asList(100)))
                    .build();
        }),
        NESTED_METRIC_WITH_WHERE_QUERY (() -> {
            Map<String, Argument> arguments = new HashMap<>();
            arguments.put("grain", Argument.builder().name("grain").value(TimeGrain.MONTH).build());
            // Sorting
            return Query.builder()
                    .source(playerStatsTable)
                    .metricProjection(playerStatsTable.getMetricProjection("dailyAverageScorePerPeriod"))
                    .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                    .timeDimensionProjection(playerStatsTable.getTimeDimensionProjection("recordedDate", arguments))
                    .whereFilter(parseFilterExpression("countryIsoCode==USA", playerStatsType, false))
                    .build();
        }),
        NESTED_METRIC_WITH_PAGINATION_QUERY (() -> {
            Map<String, Argument> arguments = new HashMap<>();
            arguments.put("grain", Argument.builder().name("grain").value(TimeGrain.MONTH).build());
            // Sorting
            return Query.builder()
                    .source(playerStatsTable)
                    .metricProjection(playerStatsTable.getMetricProjection("dailyAverageScorePerPeriod"))
                    .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                    .timeDimensionProjection(playerStatsTable.getTimeDimensionProjection("recordedDate", arguments))
                    .pagination(new ImmutablePagination(0, 1, false, true))
                    .build();
        }),
        NESTED_METRIC_WITH_SORTING_QUERY (() -> {
            Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
            sortMap.put("overallRating", Sorting.SortOrder.desc);
            sortMap.put("dailyAverageScorePerPeriod", Sorting.SortOrder.desc);

            Map<String, Argument> arguments = new HashMap<>();
            arguments.put("grain", Argument.builder().name("grain").value(TimeGrain.MONTH).build());
            // Sorting
            return Query.builder()
                    .source(playerStatsTable)
                    .metricProjection(playerStatsTable.getMetricProjection("dailyAverageScorePerPeriod"))
                    .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                    .timeDimensionProjection(playerStatsTable.getTimeDimensionProjection("recordedDate", arguments))
                    .sorting(new SortingImpl(sortMap, PlayerStats.class, dictionary))
                    .build();
        }),
        NESTED_METRIC_WITH_ALIASES_QUERY (() -> {
            Set<MetricProjection> metricProjections = new LinkedHashSet<>();
            metricProjections.add(playerStatsTable.getMetricProjection("dailyAverageScorePerPeriod", "average1"));
            metricProjections.add(playerStatsTable.getMetricProjection("dailyAverageScorePerPeriod", "average2"));

            Set<DimensionProjection> dimnesionProjections = new LinkedHashSet<>();
            dimnesionProjections.add(playerStatsTable.getDimensionProjection("overallRating", "rating"));
            dimnesionProjections.add(playerStatsTable.getDimensionProjection("playerLevel", "level"));

            Map<String, Argument> monthArgument = new HashMap<>();
            monthArgument.put("grain", Argument.builder().name("grain").value(TimeGrain.MONTH).build());
            Set<Argument> dayArgument = new HashSet<>();
            dayArgument.add(Argument.builder().name("grain").value(TimeGrain.DAY).build());

            // where filter with alias
            FilterPredicate playerLevelFilter = new FilterPredicate(
                            new Path(playerStatsType, dictionary, "playerLevel", "level", new HashSet<>()),
                            Operator.IN,
                            Arrays.asList(1, 2));
            FilterPredicate dayFilter = new FilterPredicate(
                            new Path(playerStatsType, dictionary, "recordedDate", "recordedDate", dayArgument),
                            Operator.NOTNULL,
                            new ArrayList<>());
            // forces a join to look up countryIsoCode
            FilterExpression countryIsoCodeFilter = parseFilterExpression("countryIsoCode==USA", playerStatsType, false);
            AndFilterExpression andFilterExpression = new AndFilterExpression(playerLevelFilter, countryIsoCodeFilter);
            FilterExpression wherePredicate = new AndFilterExpression(andFilterExpression, dayFilter);

            // having filter with alias
            FilterPredicate havingPredicate = new FilterPredicate(
                            new Path(playerStatsType, dictionary, "dailyAverageScorePerPeriod", "average2", new HashSet<>()),
                            Operator.GT,
                            Arrays.asList(10));

            // sorting with alias
            Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
            sortMap.put("rating", Sorting.SortOrder.desc);
            Set<Attribute> sortAttributes = new HashSet<>(Arrays.asList(Attribute.builder()
                            .type(STRING_TYPE)
                            .name("overallRating")
                            .alias("rating")
                            .build()));

            return Query.builder()
                    .source(playerStatsTable)
                    .metricProjections(metricProjections)
                    .dimensionProjections(dimnesionProjections)
                    .timeDimensionProjection(playerStatsTable.getTimeDimensionProjection("recordedDate", "byMonth", monthArgument))
                    .whereFilter(wherePredicate)
                    .havingFilter(havingPredicate)
                    .sorting(new SortingImpl(sortMap, playerStatsType, sortAttributes, dictionary))
                    .build();
        }),
        LEFT_JOIN (() ->
            Query.builder()
                    .source(videoGameTable)
                    .dimensionProjection(videoGameTable.getDimensionProjection("playerName"))
                    .build()
        ),
        INNER_JOIN (() ->
            Query.builder()
                    .source(videoGameTable)
                    .dimensionProjection(videoGameTable.getDimensionProjection("playerNameInnerJoin"))
                    .build()
        ),
        CROSS_JOIN (() ->
            Query.builder()
                    .source(videoGameTable)
                    .dimensionProjection(videoGameTable.getDimensionProjection("playerNameCrossJoin"))
                    .build()
        ),
        METRIC_JOIN(() ->
            Query.builder()
                    .source(videoGameTable)
                    .metricProjection(videoGameTable.getMetricProjection("normalizedHighScore"))
                    .build()
        );

        private Provider<Query> queryProvider;

        TestQuery(Provider<Query> p) {
            queryProvider = p;
        }

        public Query getQuery() {
            return queryProvider.get();
        }
    }

    protected Pattern repeatedWhitespacePattern = Pattern.compile("\\s\\s*");

    public static void init(SQLDialect sqlDialect, Set<Optimizer> optimizers, MetaDataStore metaDataStore) {
        Properties properties = new Properties();
        properties.put("driverClassName", "org.h2.Driver");

        String jdbcUrl = "jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1"
                + ";NON_KEYWORDS=VALUE,USER"
                + ";DATABASE_TO_UPPER=FALSE" + getCompatabilityMode(sqlDialect.getDialectType());
        properties.put("jdbcUrl", jdbcUrl);
        HikariConfig config = new HikariConfig(properties);
        DataSource dataSource = new HikariDataSource(config);

        try (Connection h2Conn = dataSource.getConnection()) {
            h2Conn.createStatement().execute("RUNSCRIPT FROM 'classpath:prepare_tables.sql'");
        } catch (SQLException e) {
            ((HikariDataSource) dataSource).close();
            throw new IllegalStateException(e);
        }

        SQLUnitTest.metaDataStore = metaDataStore;

        dictionary = EntityDictionary.builder().build();
        dictionary.bindEntity(PlayerStatsWithView.class);
        dictionary.bindEntity(PlayerStatsView.class);
        dictionary.bindEntity(PlayerStats.class);
        dictionary.bindEntity(Country.class);
        dictionary.bindEntity(SubCountry.class);
        dictionary.bindEntity(Player.class);
        dictionary.bindEntity(CountryView.class);
        dictionary.bindEntity(CountryViewNested.class);
        dictionary.bindEntity(Continent.class);
        dictionary.bindEntity(GameRevenue.class);
        filterParser = RSQLFilterDialect.builder().dictionary(dictionary).build();

        //Manually register the serdes because we are not running a complete Elide service.
        CoerceUtil.register(Day.class, new Day.DaySerde());
        CoerceUtil.register(Hour.class, new Hour.HourSerde());
        CoerceUtil.register(ISOWeek.class, new ISOWeek.ISOWeekSerde());
        CoerceUtil.register(Minute.class, new Minute.MinuteSerde());
        CoerceUtil.register(Month.class, new Month.MonthSerde());
        CoerceUtil.register(Quarter.class, new Quarter.QuarterSerde());
        CoerceUtil.register(Second.class, new Second.SecondSerde());
        CoerceUtil.register(Week.class, new Week.WeekSerde());
        CoerceUtil.register(Year.class, new Year.YearSerde());

        metaDataStore.populateEntityDictionary(dictionary);

        // Need to provide details for connections used by all available models.
        Map<String, ConnectionDetails> connectionDetailsMap = new HashMap<>();
        connectionDetailsMap.put("mycon", new ConnectionDetails(dataSource, sqlDialect));
        connectionDetailsMap.put("SalesDBConnection", new ConnectionDetails(DUMMY_DATASOURCE, sqlDialect));


        Function<String, ConnectionDetails> connectionLookup = (name) ->
                connectionDetailsMap.getOrDefault(name, new ConnectionDetails(dataSource, sqlDialect));
        engine = new SQLQueryEngine(metaDataStore, connectionLookup,
                optimizers,
                new DefaultQueryPlanMerger(metaDataStore),
                new DefaultQueryValidator(metaDataStore.getMetadataDictionary()));
        playerStatsTable = (SQLTable) metaDataStore.getTable("playerStats", NO_VERSION);
        videoGameTable = (SQLTable) metaDataStore.getTable("videoGame", NO_VERSION);
        playerStatsViewTable = (SQLTable) metaDataStore.getTable("playerStatsView", NO_VERSION);
        playerStatsViewTableArgs = new HashMap<>();
        playerStatsViewTableArgs.put("rating", Argument.builder().name("overallRating").value("Great").build());
        playerStatsViewTableArgs.put("minScore", Argument.builder().name("minScore").value("0").build());
    }

    private static String getCompatabilityMode(String dialectType) {
        if (dialectType.equals(SQLDialectFactory.getMySQLDialect().getDialectType())) {
            return ";MODE=MySQL";
        }
        if (dialectType.equals(SQLDialectFactory.getPostgresDialect().getDialectType())) {
            return ";MODE=PostgreSQL";
        }

        return "";
    }

    public static void init(SQLDialect dialect) {
        MetaDataStore metaDataStore = new MetaDataStore(DefaultClassScanner.getInstance(),
                getClassType(DefaultClassScanner.getInstance().getAnnotatedClasses("example",
                        Include.class)),
                false);
        init(dialect, new HashSet<>(), metaDataStore);
    }

    public static void init() {
        init(SQLDialectFactory.getDefaultDialect());
    }

    @BeforeEach
    public void begin() {
        transaction = engine.beginTransaction();
    }

    @AfterEach
    public void end() {
        transaction.close();
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
        if (expected == actual) {
            return;
        }
        assertNotNull(expected, "Expected a null query List, but actual was non-null");
        assertNotNull(actual, "Expected a non-null query List, but actual was null");
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
    protected static FilterExpression parseFilterExpression(String expressionText, Type<?> entityType,
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

    protected void testQueryExecution(Query query) {
        try (QueryEngine.Transaction transaction = engine.beginTransaction()) {
            assertDoesNotThrow(() -> engine.executeQuery(query, transaction));
        }
    }

    protected String getExpectedNestedMetricWithSortingQuery(boolean useAliasForOrderByClause) {
        String expected = "SELECT AVG(`example_PlayerStats_XXX`.`highScore`) "
                + "AS `dailyAverageScorePerPeriod`,`example_PlayerStats_XXX`.`overallRating` AS `overallRating`,"
                + "`example_PlayerStats_XXX`.`recordedDate` AS `recordedDate` "
                + "FROM (SELECT MAX(`example_PlayerStats`.`highScore`) AS `highScore`,"
                + "`example_PlayerStats`.`overallRating` AS `overallRating`,"
                + "PARSEDATETIME(FORMATDATETIME(`example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') AS `recordedDate_XXX`,"
                + "PARSEDATETIME(FORMATDATETIME(`example_PlayerStats`.`recordedDate`, 'yyyy-MM-01'), 'yyyy-MM-dd') AS `recordedDate` "
                + "FROM `playerStats` AS `example_PlayerStats` GROUP BY "
                + "`example_PlayerStats`.`overallRating`, "
                + "PARSEDATETIME(FORMATDATETIME(`example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd'), "
                + "PARSEDATETIME(FORMATDATETIME(`example_PlayerStats`.`recordedDate`, 'yyyy-MM-01'), 'yyyy-MM-dd') ) "
                + "AS `example_PlayerStats_XXX` GROUP BY "
                + "`example_PlayerStats_XXX`.`overallRating`, "
                + "`example_PlayerStats_XXX`.`recordedDate` ";

        if (useAliasForOrderByClause) {
            expected = expected + "ORDER BY `dailyAverageScorePerPeriod` DESC,`overallRating` DESC";

        } else {
            expected = expected
                    + "ORDER BY AVG(`example_PlayerStats_XXX`.`highScore`) DESC,"
                    + "`example_PlayerStats_XXX`.`overallRating` DESC";
        }
        return expected;
    }

    protected String getExpectedNestedMetricWithHavingQuery() {
        return "SELECT AVG(`example_PlayerStats_XXX`.`highScore`) "
                + "AS `dailyAverageScorePerPeriod`,`example_PlayerStats_XXX`.`overallRating` AS `overallRating`,"
                + "`example_PlayerStats_XXX`.`recordedDate` AS `recordedDate` "
                + "FROM (SELECT MAX(`example_PlayerStats`.`highScore`) AS `highScore`,"
                + "`example_PlayerStats`.`overallRating` AS `overallRating`,"
                + "PARSEDATETIME(FORMATDATETIME(`example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') AS `recordedDate_XXX`,"
                + "PARSEDATETIME(FORMATDATETIME(`example_PlayerStats`.`recordedDate`, 'yyyy-MM-01'), 'yyyy-MM-dd') AS `recordedDate` "
                + "FROM `playerStats` AS `example_PlayerStats` GROUP BY "
                + "`example_PlayerStats`.`overallRating`, "
                + "PARSEDATETIME(FORMATDATETIME(`example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd'), "
                + "PARSEDATETIME(FORMATDATETIME(`example_PlayerStats`.`recordedDate`, 'yyyy-MM-01'), 'yyyy-MM-dd') ) "
                + "AS `example_PlayerStats_XXX` GROUP BY "
                + "`example_PlayerStats_XXX`.`overallRating`, "
                + "`example_PlayerStats_XXX`.`recordedDate` "
                + "HAVING AVG(`example_PlayerStats_XXX`.`highScore`) "
                + "> :XXX\n";
    }

    protected String getExpectedNestedMetricWithWhereQuery() {
        return "SELECT AVG(`example_PlayerStats_XXX`.`highScore`) "
                + "AS `dailyAverageScorePerPeriod`,`example_PlayerStats_XXX`.`overallRating` AS `overallRating`,"
                + "`example_PlayerStats_XXX`.`recordedDate` AS `recordedDate` "
                + "FROM (SELECT MAX(`example_PlayerStats`.`highScore`) AS `highScore`,"
                + "`example_PlayerStats`.`overallRating` AS `overallRating`,"
                + "PARSEDATETIME(FORMATDATETIME(`example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') AS `recordedDate_XXX`,"
                + "PARSEDATETIME(FORMATDATETIME(`example_PlayerStats`.`recordedDate`, 'yyyy-MM-01'), 'yyyy-MM-dd') AS `recordedDate` "
                + "FROM `playerStats` AS `example_PlayerStats` "
                + "LEFT OUTER JOIN `countries` AS `example_PlayerStats_country_XXX` ON `example_PlayerStats`.`country_id` = `example_PlayerStats_country_XXX`.`id` "
                + "WHERE `example_PlayerStats_country_XXX`.`iso_code` IN (:XXX) "
                + "GROUP BY `example_PlayerStats`.`overallRating`, "
                + "PARSEDATETIME(FORMATDATETIME(`example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd'), "
                + "PARSEDATETIME(FORMATDATETIME(`example_PlayerStats`.`recordedDate`, 'yyyy-MM-01'), 'yyyy-MM-dd') ) "
                + "AS `example_PlayerStats_XXX` GROUP BY "
                + "`example_PlayerStats_XXX`.`overallRating`, "
                + "`example_PlayerStats_XXX`.`recordedDate`\n";
    }

    protected String getExpectedNestedMetricQuery() {
        return "SELECT AVG(`example_PlayerStats_XXX`.`highScore`) AS `dailyAverageScorePerPeriod`,"
                        + "MAX(`example_PlayerStats_XXX`.`INNER_AGG_XXX`) AS `highScore`,"
                        + "MIN(`example_PlayerStats_XXX`.`INNER_AGG_XXX`) AS `lowScore`,"
                        + "`example_PlayerStats_XXX`.`overallRating` AS `overallRating`,"
                        + "`example_PlayerStats_XXX`.`recordedDate` AS `recordedDate` "
                        + "FROM (SELECT MAX(`example_PlayerStats`.`highScore`) AS `highScore`,"
                        + "MAX(`example_PlayerStats`.`highScore`) AS `INNER_AGG_XXX`,"
                        + "MIN(`example_PlayerStats`.`lowScore`) AS `INNER_AGG_XXX`,"
                        + "`example_PlayerStats`.`overallRating` AS `overallRating`,"
                        + "PARSEDATETIME(FORMATDATETIME(`example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') AS `recordedDate_XXX`,"
                        + "PARSEDATETIME(FORMATDATETIME(`example_PlayerStats`.`recordedDate`, 'yyyy-MM-01'), 'yyyy-MM-dd') AS `recordedDate` "
                        + "FROM `playerStats` AS `example_PlayerStats` GROUP BY "
                        + "`example_PlayerStats`.`overallRating`, "
                        + "PARSEDATETIME(FORMATDATETIME(`example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd'), "
                        + "PARSEDATETIME(FORMATDATETIME(`example_PlayerStats`.`recordedDate`, 'yyyy-MM-01'), 'yyyy-MM-dd') ) "
                        + "AS `example_PlayerStats_XXX` GROUP BY "
                        + "`example_PlayerStats_XXX`.`overallRating`, "
                        + "`example_PlayerStats_XXX`.`recordedDate`\n";

    }

    protected String getExpectedNestedMetricWithAliasesSQL(boolean useAliasForOrderByClause) {

        String expectedSQL =
                          "SELECT \n"
                        + "    AVG(`example_PlayerStats_XXX`.`highScore`) AS `dailyAverageScorePerPeriod_165126357`,\n"
                        + "    AVG(`example_PlayerStats_XXX`.`highScore`) AS `dailyAverageScorePerPeriod_165126481`,\n"
                        + "    `example_PlayerStats_XXX`.`overallRating_207658499` AS `overallRating_207658499`,\n"
                        + "    `example_PlayerStats_XXX`.`playerLevel_96024484` AS `playerLevel_96024484`,\n"
                        + "    `example_PlayerStats_XXX`.`recordedDate_155839778` AS `recordedDate_155839778` \n"
                        + "FROM \n"
                        + "    (\n"
                        + "        SELECT \n"
                        + "            MAX(`example_PlayerStats`.`highScore`) AS `highScore`,\n"
                        + "            `example_PlayerStats`.`overallRating` AS `overallRating_207658499`,\n"
                        + "            CASE WHEN `example_PlayerStats`.`overallRating` = 'Good' THEN 1 ELSE 2 END AS `playerLevel_96024484`,\n"
                        + "            PARSEDATETIME(FORMATDATETIME(`example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') AS `recordedDate_55557339`,\n"
                        + "            PARSEDATETIME(FORMATDATETIME(`example_PlayerStats`.`recordedDate`, 'yyyy-MM-01'), 'yyyy-MM-dd') AS `recordedDate_155839778` \n"
                        + "        FROM \n"
                        + "            `playerStats` AS `example_PlayerStats` \n"
                        + "            LEFT OUTER JOIN \n"
                        + "            `countries` AS `example_PlayerStats_country_XXX` \n"
                        + "            ON \n"
                        + "            `example_PlayerStats`.`country_id` = `example_PlayerStats_country_XXX`.`id` \n"
                        + "        WHERE \n"
                        + "            (\n"
                        + "                (CASE WHEN `example_PlayerStats`.`overallRating` = 'Good' THEN 1 ELSE 2 END IN (:XXX, :XXX) \n"
                        + "                AND \n"
                        + "                `example_PlayerStats_country_XXX`.`iso_code` IN (:XXX)) \n"
                        + "                AND \n"
                        + "                PARSEDATETIME(FORMATDATETIME(`example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') IS NOT NULL\n"
                        + "            ) \n"
                        + "        GROUP BY \n"
                        + "            `example_PlayerStats`.`overallRating`, \n"
                        + "            CASE WHEN `example_PlayerStats`.`overallRating` = 'Good' THEN 1 ELSE 2 END, \n"
                        + "            PARSEDATETIME(FORMATDATETIME(`example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd'), \n"
                        + "            PARSEDATETIME(FORMATDATETIME(`example_PlayerStats`.`recordedDate`, 'yyyy-MM-01'), 'yyyy-MM-dd') \n"
                        + "    ) AS `example_PlayerStats_XXX` \n"
                        + "GROUP BY \n"
                        + "    `example_PlayerStats_XXX`.`overallRating_207658499`, \n"
                        + "    `example_PlayerStats_XXX`.`playerLevel_96024484`, \n"
                        + "    `example_PlayerStats_XXX`.`recordedDate_155839778` \n"
                        + "HAVING \n"
                        + "    AVG(`example_PlayerStats_XXX`.`highScore`) > :XXX \n"
                        + "ORDER BY \n"
                        + "    `example_PlayerStats_XXX`.`overallRating_207658499` DESC ";

        if (useAliasForOrderByClause) {
            // Changing last line to have alias only.
            String[] split = expectedSQL.split(NEWLINE);
            split[split.length - 1] = "`overallRating_207658499` DESC ";
            expectedSQL = String.join(NEWLINE, split);
        }
        return formatInSingleLine(expectedSQL);
    }

    protected String getExpectedWhereWithArgumentsSQL() {

        String expectedSQL =
                          "SELECT \n"
                        + "    MAX(`example_PlayerStats`.`highScore`) AS `highScore`,\n"
                        + "    PARSEDATETIME(FORMATDATETIME(`example_PlayerStats`.`recordedDate`, 'yyyy-MM-01'), 'yyyy-MM-dd') AS `recordedDate` \n"
                        + "FROM \n"
                        + "    `playerStats` AS `example_PlayerStats` \n"
                        + "WHERE \n"
                        + "     PARSEDATETIME(FORMATDATETIME(`example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') IS NOT NULL \n"
                        + "GROUP BY \n"
                        + "     PARSEDATETIME(FORMATDATETIME(`example_PlayerStats`.`recordedDate`, 'yyyy-MM-01'), 'yyyy-MM-dd') ";

        return formatInSingleLine(expectedSQL);
    }

    private String formatInSingleLine(String expectedSQL) {
        // Remove spaces at the start of each line with in string
        expectedSQL = expectedSQL.replaceAll("(?m)^\\s*", "");
        expectedSQL = expectedSQL.replace(NEWLINE, "");
        return repeatedWhitespacePattern.matcher(expectedSQL).replaceAll(" ");
    }
}
