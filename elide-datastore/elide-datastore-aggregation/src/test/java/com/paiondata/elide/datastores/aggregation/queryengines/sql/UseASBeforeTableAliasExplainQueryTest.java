/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.aggregation.queryengines.sql;

import com.paiondata.elide.datastores.aggregation.framework.SQLUnitTest;
import com.paiondata.elide.datastores.aggregation.query.Query;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.dialects.AbstractSqlDialect;
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
                        + "(SELECT `example_PlayerStats`.`overallRating`, "
                        + "PARSEDATETIME(FORMATDATETIME(`example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') "
                        + "FROM `playerStats` `example_PlayerStats` "
                        + "GROUP BY `example_PlayerStats`.`overallRating`, "
                        + "PARSEDATETIME(FORMATDATETIME(`example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') ) `pagination_subquery`";

        String expectedQueryStr2 =
                "SELECT MIN(`example_PlayerStats`.`lowScore`) AS "
                        + "`lowScore`,`example_PlayerStats`.`overallRating` AS "
                        + "`overallRating`,PARSEDATETIME(FORMATDATETIME("
                        + "`example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), "
                        + "'yyyy-MM-dd') AS `recordedDate` FROM `playerStats` "
                        + "`example_PlayerStats`   "
                        + "GROUP BY `example_PlayerStats`.`overallRating`, "
                        + "PARSEDATETIME(FORMATDATETIME("
                        + "`example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), "
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
                        "SELECT DISTINCT `example_VideoGame_playerInnerJoin_XXX`.`name` AS `playerNameInnerJoin` FROM `videoGames` `example_VideoGame`"
                                        + " INNER JOIN `players` `example_VideoGame_playerInnerJoin_XXX` ON `example_VideoGame`.`player_id`"
                                        + " = `example_VideoGame_playerInnerJoin_XXX`.`id`";

        compareQueryLists(expectedQueryStr, engine.explain(query));
    }
}
