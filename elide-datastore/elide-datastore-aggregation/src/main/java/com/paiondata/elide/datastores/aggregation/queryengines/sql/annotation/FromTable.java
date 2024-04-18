/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.queryengines.sql.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the entity is derived directly from a physical table or view in the database.
 */
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface FromTable {

    /**
     * The table or view name.
     *
     * @return The table or view name.
     */
    String name();

    /**
     * DB Connection Name for this table.
     * @return String DB Connection Name
     */
    // TO DO
    String dbConnectionName() default "";
}
