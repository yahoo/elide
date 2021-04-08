/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql;


import static com.yahoo.elide.datastores.aggregation.queryengines.sql.TableContextTest.emptyMap;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.TableContextTest.toMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;

import org.junit.jupiter.api.Test;

public class TableContextSQLHelperTest {

    private TableContext revenueFact;

    public TableContextSQLHelperTest() {

        // Prepare revenueFact context
        revenueFact = TableContext.builder()
                        .alias("revenueFact")
                        .dialect(SQLDialectFactory.getH2Dialect())
                        .defaultTableArgs(toMap("denominator", "1000"))
                        .build();

        ColumnDefinition impressions = new ColumnDefinition(
                        "{{$$column.args.aggregation}}({{$impressions}})",
                        toMap("aggregation", "MAX"));

        ColumnDefinition revenue = new ColumnDefinition(
                        "TO_CHAR(SUM({{$revenue}}) * {{rates.conversionRate}}, {{$$column.args.format}})",
                        toMap("format", "999999D00"));

        // Using sql helper for invoking 'conversionRate' in 'rates' table
        ColumnDefinition revenueUsingSqlHelper = new ColumnDefinition(
                        "TO_CHAR(SUM({{$revenue}}) * {{sql from='rates' column='conversionRate'}}, {{$$column.args.format}})",
                        toMap("format", "999999D00"));

        ColumnDefinition impressionsPerUSD = new ColumnDefinition(
                        "{{sql column='impressions[aggregation:SUM]'}} / {{sql column='revenue[format:999999D0000]'}}",
                        emptyMap());

        revenueFact.put("impressions", impressions);
        revenueFact.put("revenue", revenue);
        revenueFact.put("revenueUsingSqlHelper", revenueUsingSqlHelper);
        revenueFact.put("impressionsPerUSD", impressionsPerUSD);


        // Prepare currencyRates context
        TableContext currencyRates = TableContext.builder()
                        .alias("revenueFact_currencyRates")
                        .dialect(SQLDialectFactory.getH2Dialect())
                        .defaultTableArgs(emptyMap())
                        .build();

        ColumnDefinition conversionRate = new ColumnDefinition(
                        "TO_CHAR({{$conversion_rate}}, {{$$column.args.format}})",
                        toMap("format", "999D00"));

        currencyRates.put("conversionRate", conversionRate);


        // Link tables
        revenueFact.addJoinContext("rates", currencyRates);
    }

    @Test
    public void testTableContext() {

        assertEquals("MAX(`revenueFact`.`impressions`)",
                     revenueFact.get("impressions"));

        // default value of 'format' argument is used for 'conversion_rate' column
        assertEquals("TO_CHAR(SUM(`revenueFact`.`revenue`) * TO_CHAR(`revenueFact_rates`.`conversion_rate`, 999D00), "
                        + "999999D00)",
                     revenueFact.get("revenue"));

        // invoking column's value of 'format' argument is used for 'conversion_rate' column
        assertEquals("TO_CHAR(SUM(`revenueFact`.`revenue`) * TO_CHAR(`revenueFact_rates`.`conversion_rate`, 999999D00), "
                        + "999999D00)",
                     revenueFact.get("revenueUsingSqlHelper"));

        assertEquals("SUM(`revenueFact`.`impressions`) / TO_CHAR(SUM(`revenueFact`.`revenue`) * "
                        + "TO_CHAR(`revenueFact_rates`.`conversion_rate`, 999D00), 999999D0000)",
                     revenueFact.get("impressionsPerUSD"));
    }
}
