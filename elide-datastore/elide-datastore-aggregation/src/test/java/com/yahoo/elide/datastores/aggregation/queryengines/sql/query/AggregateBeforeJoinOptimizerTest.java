/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import static com.yahoo.elide.core.utils.TypeHelper.getClassType;
import com.yahoo.elide.core.utils.ClassScanner;
import com.yahoo.elide.datastores.aggregation.framework.SQLUnitTest;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.query.Optimizer;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.impl.H2Dialect;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AggregateBeforeJoinOptimizerTest extends SQLUnitTest {

    @BeforeAll
    public static void init() {
        MetaDataStore metaDataStore =new MetaDataStore(
                getClassType(ClassScanner.getAllClasses("com.yahoo.elide.datastores.aggregation.example")),
                false);
        Set<Optimizer> optimizers = new HashSet<>(Arrays.asList(new AggregateBeforeJoinOptimizer(metaDataStore)));
        init(new H2Dialect(), optimizers, metaDataStore);
    }

    @Test
    void testWhereAnd() {
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
}
