/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql;

import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.COL_PREFIX;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.TBL_PREFIX;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.prepareArgumentsMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

public class TableContextTest {

    private TableContext orderDetails;

    public TableContextTest() {

        // Prepare order context
        orderDetails = TableContext.builder()
                        .alias("OrderDetails")
                        .dialect(SQLDialectFactory.getH2Dialect())
                        .defaultTableArgs(prepareArgumentsMap(toMap("denominator", "1000"), TBL_PREFIX))
                        .build();

        ColumnDefinition customerState = new ColumnDefinition(
                        "{{customer.stateName}} AND {{customer.zipCode}} * {{$$column.args.multiplier}} / {{$$table.args.denominator}}",
                        prepareArgumentsMap(toMap("multiplier", "26"), COL_PREFIX));

        ColumnDefinition customerStateName = new ColumnDefinition(
                        "{{customer.region.name}} OR {{customer.region.$name}}",
                        prepareArgumentsMap(emptyMap(), COL_PREFIX));

        ColumnDefinition cost = new ColumnDefinition(
                        "{{delivery.deliveryCost}} * {{$$column.args.multiplier}} + {{$orderTotal}} / {{$$table.args.denominator}}",
                        prepareArgumentsMap(toMap("multiplier", "0.1"), COL_PREFIX));

        orderDetails.put("customerState", customerState);
        orderDetails.put("customerStateName", customerStateName);
        orderDetails.put("cost", cost);

        // Prepare delivery context
        TableContext delivery = TableContext.builder()
                        .alias("OrderDetails_delivery")
                        .dialect(SQLDialectFactory.getH2Dialect())
                        .defaultTableArgs(prepareArgumentsMap(emptyMap(), TBL_PREFIX))
                        .build();

        ColumnDefinition deliveryCost = new ColumnDefinition(
                        "{{$cost}} * {{$$column.args.multiplier}}",
                        prepareArgumentsMap(toMap("multiplier", "1.0"), COL_PREFIX));

        delivery.put("deliveryCost", deliveryCost);

        // Prepare customer context
        TableContext customer = TableContext.builder()
                        .alias("OrderDetails_customer")
                        .dialect(SQLDialectFactory.getH2Dialect())
                        .defaultTableArgs(prepareArgumentsMap(toMap("minZipCode", "10001"), TBL_PREFIX))
                        .build();

        ColumnDefinition stateName = new ColumnDefinition(
                        "{{region.name}} OR {{region.$name}} == '{{$$column.args.customerRegion}}' OR {{zipCode}} > {{$$table.args.minZipCode}}",
                        prepareArgumentsMap(toMap("customerRegion", "Virginia"), COL_PREFIX));

        ColumnDefinition zipCode = new ColumnDefinition(
                        "{{$zipCode}}",
                        prepareArgumentsMap(emptyMap(), COL_PREFIX));

        customer.put("stateName", stateName);
        customer.put("zipCode", zipCode);

        // Prepare region context
        TableContext region = TableContext.builder()
                        .alias("OrderDetails_customer_region")
                        .dialect(SQLDialectFactory.getH2Dialect())
                        .defaultTableArgs(prepareArgumentsMap(toMap("stateNamePrefix", "New"), TBL_PREFIX))
                        .build();

        ColumnDefinition name = new ColumnDefinition(
                        "{{$name}} starts with '{{$$table.args.stateNamePrefix}}'",
                        prepareArgumentsMap(toMap("defaultState", "Virginia"), COL_PREFIX));
        region.put("name", name);

        // Link tables
        customer.addJoinContext("region", region);
        orderDetails.addJoinContext("customer", customer);
        orderDetails.addJoinContext("delivery", delivery);
    }

    @Test
    public void testTableContext() {

        assertEquals("`OrderDetails_delivery`.`cost` * 1.0 * 0.1 + `OrderDetails`.`orderTotal` / 1000",
                     orderDetails.get("cost"));

        assertEquals("`OrderDetails_customer_region`.`name` starts with 'New' OR "
                        + "`OrderDetails_customer_region`.`name` == 'Virginia' OR "
                        + "`OrderDetails_customer`.`zipCode` > 10001 AND "
                        + "`OrderDetails_customer`.`zipCode` * 26 / 1000",
                     orderDetails.get("customerState"));

        assertEquals("`OrderDetails_customer_region`.`name` starts with 'New' OR "
                        + "`OrderDetails_customer_region`.`name`",
                     orderDetails.get("customerStateName"));
    }

    public static Map<String, Object> toMap(String key, String value) {
        return Collections.singletonMap(key, value);
    }

    public static Map<String, Object> emptyMap() {
        return Collections.emptyMap();
    }
}
