/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.hibernate.hql;

import com.yahoo.elide.request.Relationship;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Holds all the properties of an entity relationship.
 */
@Data
@AllArgsConstructor
public class RelationshipImpl implements AbstractHQLQueryBuilder.Relationship {
    private Class<?> parentType;
    private Object parent;
    private Relationship relationship;
}
