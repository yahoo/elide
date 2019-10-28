package com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * View indicates the name of this view and the sql expression to select it. Only for models that don't have id field.
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface View {
    /**
     * Name defined for this view.
     * @return view name
     */
    String name() default "";

    /**
     * Table name or select statement to construct this view.
     * @return sql statement
     */
    String from() default "";
}
