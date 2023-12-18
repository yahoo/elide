/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.predicates;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.Path.PathElement;
import com.yahoo.elide.core.filter.Operator;

import java.util.Collections;

/**
 * HasMember Predicate class.
 * <p>
 * This determines if a collection association path contains a particular value
 * in the collection.
 * <p>
 * This can be used to determine if an entity in a to-many association does not
 * have a null field value in the collection.
 */
public class HasMemberPredicate extends FilterPredicate {

    /**
     * Determines if a collection association path contains a particular value in
     * the collection.
     *
     * @param path  the collection association path
     * @param value to verify is a member which can be null to check if an entity in
     *              a to-many association has a null field value
     */
    public HasMemberPredicate(Path path, Object value) {
        super(path, Operator.HASMEMBER, Collections.singletonList(value));
    }

    /**
     * Determines if a collection association path contains a particular value in
     * the collection.
     *
     * @param pathElement the collection association path
     * @param value       to verify is a member which can be null to check if an
     *                    entity in a to-many association has a null field value
     */
    public HasMemberPredicate(PathElement pathElement, Object value) {
        this(new Path(Collections.singletonList(pathElement)), value);
    }
}
