/*
 * Copyright 2022, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example.tables;

import com.paiondata.elide.datastores.aggregation.query.Query;
import com.paiondata.elide.datastores.aggregation.query.TableSQLMaker;

public class OrderDetailsMaker implements TableSQLMaker {
    /**
     * Creates a subquery that mirrors the following table.
     * <pre>
     * CREATE TABLE IF NOT EXISTS order_details
     * (
     *   order_id VARCHAR(255) NOT NULL,
     *   customer_id VARCHAR(255),
     *   order_total NUMERIC(10,2),
     *   created_on DATETIME,
     *   PRIMARY KEY (order_id)
     * );
     * </pre>
     * @param clientQuery the client query.
     * @return SQL query
     */
    @Override
    public String make(Query clientQuery) {
        return "SELECT order_id, customer_id, order_total, created_on FROM order_details";
    }
}
