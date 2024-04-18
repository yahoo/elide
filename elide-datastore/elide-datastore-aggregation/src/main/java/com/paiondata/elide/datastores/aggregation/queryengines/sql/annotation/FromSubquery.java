/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.queryengines.sql.annotation;

import com.paiondata.elide.datastores.aggregation.query.DefaultTableSQLMaker;
import com.paiondata.elide.datastores.aggregation.query.TableSQLMaker;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the entity or field is derived from a native SQL subquery.
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface FromSubquery {
    /**
     * The SQL subquery.
     *
     * @return The SQL subquery.
     */
    String sql();

    /**
     * Generates the subquery SQL dynamically.
     *
     * @return The class of the subquery generator.
     */
    Class<? extends TableSQLMaker> maker() default DefaultTableSQLMaker.class;

    /**
     * DB Connection Name for this query.
     * @return String DB Connection Name
     */
    // TO DO
    String dbConnectionName() default "";
}
