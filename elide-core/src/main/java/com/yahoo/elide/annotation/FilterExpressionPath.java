/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Builds multi-bean properties path for FilterExpressionCheck.
 * <pre>
 * <code>
 *  &#64;Transient
 *  &#64;ComputedRelationship
 *  &#64;OneToOne
 *  &#64;FilterExpressionPath("publisher.editor")
 *  &#64;ReadPermission(expression = "Field path editor check")
 *  public Author getEditor() {
 *    return getPublisher().getEditor();
 *  }
 * </code>
 * </pre>
 */
@Target({ METHOD, FIELD })
@Retention(RUNTIME)
public @interface FilterExpressionPath {
    String value();
}
