/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import static com.yahoo.elide.core.utils.TypeHelper.getClassType;
import static org.junit.jupiter.api.Assertions.assertFalse;
import com.yahoo.elide.core.utils.ClassScanner;
import com.yahoo.elide.datastores.aggregation.framework.SQLUnitTest;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.query.Optimizer;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.impl.H2Dialect;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.DynamicSQLReferenceTable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AggregateBeforeJoinOptimizerTest extends SQLUnitTest {

    @BeforeAll
    public static void init() {
        MetaDataStore metaDataStore = new MetaDataStore(
                getClassType(ClassScanner.getAllClasses("com.yahoo.elide.datastores.aggregation.example")),
                false);
        Set<Optimizer> optimizers = new HashSet<>(Arrays.asList(new AggregateBeforeJoinOptimizer(metaDataStore)));
        init(new H2Dialect(), optimizers, metaDataStore);
    }

    @Test
    public void testWhereAnd() {
        Query query = TestQuery.WHERE_AND.getQuery();
        String expectedQueryStr =
                "SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`highScore`) AS `highScore`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`overallRating` AS `overallRating` "
                        + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`highScore`) AS `highScore`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`overallRating` AS `overallRating`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`country_id` AS `country_id` "
                        + "FROM `playerStats` AS `com_yahoo_elide_datastores_aggregation_example_PlayerStats` "
                        + "WHERE `com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`overallRating` IS NOT NULL "
                        + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`overallRating`, "
                        + "`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`country_id` ) AS `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX` "
                        + "LEFT OUTER JOIN `countries` AS `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX_country` "
                        + "ON `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`country_id` = `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX_country`.`id` "
                        + "WHERE `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX_country`.`iso_code` IN (:XXX) "
                        + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`overallRating`\n";

        compareQueryLists(expectedQueryStr, engine.explain(query));
    }

    @Test
    public void testSortAndPaginate() {
        Query query = TestQuery.COMPLICATED.getQuery();

        String expectedQueryStr1 =
                "SELECT COUNT(*) FROM "
                + "(SELECT `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`overallRating`, "
                        + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') "
                        + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`highScore`) AS `highScore`,"
                        + "MIN(`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`lowScore`) AS `lowScore`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`overallRating` AS `overallRating`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`country_id` AS `country_id`,"
                        + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') AS `recordedDate` "
                        + "FROM `playerStats` AS `com_yahoo_elide_datastores_aggregation_example_PlayerStats` "
                        + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`overallRating`, "
                        + "`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`country_id`, "
                        + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') ) "
                        + "AS `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX` "
                        + "LEFT OUTER JOIN `countries` AS `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX_country` "
                        + "ON `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`country_id` = `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX_country`.`id` "
                        + "WHERE `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX_country`.`iso_code` IN (:XXX) "
                        + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`overallRating`, "
                        + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') "
                        + "HAVING MIN(`com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`lowScore`) > :XXX ) AS `pagination_subquery`\n";

        String expectedQueryStr2 =
                "SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`highScore`) AS `highScore`,"
                        + "MIN(`com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`lowScore`) AS `lowScore`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`overallRating` AS `overallRating`,"
                        + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') AS `recordedDate` "
                        + "FROM (SELECT MAX(`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`highScore`) AS `highScore`,"
                        + "MIN(`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`lowScore`) AS `lowScore`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`overallRating` AS `overallRating`,"
                        + "`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`country_id` AS `country_id`,"
                        + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') AS `recordedDate` "
                        + "FROM `playerStats` AS `com_yahoo_elide_datastores_aggregation_example_PlayerStats` "
                        + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`overallRating`, "
                        + "`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`country_id`, "
                        + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') ) "
                        + "AS `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX` "
                        + "LEFT OUTER JOIN `countries` AS `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX_country` "
                        + "ON `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`country_id` = `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX_country`.`id` "
                        + "WHERE `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX_country`.`iso_code` IN (:XXX) "
                        + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`overallRating`, "
                        + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') "
                        + "HAVING MIN(`com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`lowScore`) > :XXX "
                        + "ORDER BY MIN(`com_yahoo_elide_datastores_aggregation_example_PlayerStats_XXX`.`lowScore`) "
                        + "DESC LIMIT 5 OFFSET 10\n";
        List<String> expectedQueryList = new ArrayList<String>();
        expectedQueryList.add(expectedQueryStr1);
        expectedQueryList.add(expectedQueryStr2);

        compareQueryLists(expectedQueryList, engine.explain(query));

        testQueryExecution(TestQuery.COMPLICATED.getQuery());
    }

    @Test
    public void testNoOptimizationNoJoins() {
        Query query = TestQuery.WHERE_DIMS_ONLY.getQuery();

        AggregateBeforeJoinOptimizer optimizer = new AggregateBeforeJoinOptimizer(metaDataStore);

        DynamicSQLReferenceTable lookupTable = new DynamicSQLReferenceTable(engine.getReferenceTable(), query);

        assertFalse(optimizer.canOptimize(query, lookupTable));
    }

    @Test
    public void testNoOptimizationNestedQuery() {
        Query query = TestQuery.NESTED_METRIC_WITH_WHERE_QUERY.getQuery();

        AggregateBeforeJoinOptimizer optimizer = new AggregateBeforeJoinOptimizer(metaDataStore);

        DynamicSQLReferenceTable lookupTable = new DynamicSQLReferenceTable(engine.getReferenceTable(), query);

        assertFalse(optimizer.canOptimize(query, lookupTable));
    }
}
