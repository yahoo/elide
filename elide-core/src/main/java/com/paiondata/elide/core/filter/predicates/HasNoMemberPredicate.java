/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.filter.predicates;

import com.paiondata.elide.core.Path;
import com.paiondata.elide.core.Path.PathElement;
import com.paiondata.elide.core.filter.Operator;

import java.util.Collections;

/**
 * HasNoMember Predicate class.
 * <p>
 * This determines if a collection association path does not contain a
 * particular value in the collection.
 * <p>
 * This can be used to determine if an entity in a to-many association does not
 * have a null field value in the collection.
 */
public class HasNoMemberPredicate extends FilterPredicate {

    /**
     * Determines if a collection association path does not contain a particular
     * value in the collection.
     *
     * @param path  the collection association path
     * @param value to verify is not a a member which can be null to check if an
     *              entity in to-many association does not have a null field value
     */
    public HasNoMemberPredicate(Path path, Object value) {
        super(path, Operator.HASNOMEMBER, Collections.singletonList(value));
    }

    /**
     * Determines if a collection association path does not contain a particular
     * value in the collection.
     *
     * @param pathElement the collection association path
     * @param value       to verify is not a member which can be null to check if an
     *                    entity in a to-many association does not have a null field
     *                    value
     */
    public HasNoMemberPredicate(PathElement pathElement, Object value) {
        this(new Path(Collections.singletonList(pathElement)), value);
    }
}
