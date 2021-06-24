/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql;

import com.yahoo.elide.datastores.aggregation.framework.SQLUnitTest;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.AbstractSqlDialect;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class UseASBeforeTableAliasExplainQueryTest extends SQLUnitTest {

    @BeforeAll
    public static void init() {
        SQLUnitTest.init(new AbstractSqlDialect() {

            @Override
            public String getDialectType() {
                return "test";
            }

            @Override
            public boolean useASBeforeTableAlias() {
                return false;
            }
        });
    }

    @Test
    public void testExplainPagination() {
        String expectedQueryStr1 = "SELECT COUNT(*) FROM "
                        + "(SELECT `com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`overallRating`, "
                        + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') "
                        + "FROM `playerStats` `com_yahoo_elide_datastores_aggregation_example_PlayerStats` "
                        + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`overallRating`, "
                        + "PARSEDATETIME(FORMATDATETIME(`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') ) `pagination_subquery`";

        String expectedQueryStr2 =
                "SELECT MIN(`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`lowScore`) AS "
                        + "`lowScore`,`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`overallRating` AS "
                        + "`overallRating`,PARSEDATETIME(FORMATDATETIME("
                        + "`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), "
                        + "'yyyy-MM-dd') AS `recordedDate` FROM `playerStats` "
                        + "`com_yahoo_elide_datastores_aggregation_example_PlayerStats`   "
                        + "GROUP BY `com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`overallRating`, "
                        + "PARSEDATETIME(FORMATDATETIME("
                        + "`com_yahoo_elide_datastores_aggregation_example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), "
                        + "'yyyy-MM-dd') OFFSET 0 LIMIT 1";
        List<String> expectedQueryList = new ArrayList<>();
        expectedQueryList.add(expectedQueryStr1);
        expectedQueryList.add(expectedQueryStr2);
        compareQueryLists(expectedQueryList, engine.explain(TestQuery.PAGINATION_TOTAL.getQuery()));
    }

    @Test
    public void testInnerJoin() throws Exception {
        Query query = TestQuery.INNER_JOIN.getQuery();

        String expectedQueryStr =
                        "SELECT DISTINCT `com_yahoo_elide_datastores_aggregation_example_VideoGame_playerInnerJoin_XXX`.`name` AS `playerNameInnerJoin` FROM `videoGames` `com_yahoo_elide_datastores_aggregation_example_VideoGame`"
                                        + " INNER JOIN `players` `com_yahoo_elide_datastores_aggregation_example_VideoGame_playerInnerJoin_XXX` ON `com_yahoo_elide_datastores_aggregation_example_VideoGame`.`player_id`"
                                        + " = `com_yahoo_elide_datastores_aggregation_example_VideoGame_playerInnerJoin_XXX`.`id`";

        compareQueryLists(expectedQueryStr, engine.explain(query));
    }
}
