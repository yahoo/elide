/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.export.validator;

import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.request.EntityProjection;

import java.util.Collection;

/**
 * Validates none of the projections have relationships.
 */
public class NoRelationshipsProjectionValidator implements Validator {

    private static final Validator INSTANCE = new NoRelationshipsProjectionValidator();

    public static Validator getInstance() {
        return INSTANCE;
    }

    @Override
    public void validateProjection(Collection<EntityProjection> projections) {
        for (EntityProjection projection : projections) {
            if (!projection.getRelationships().isEmpty()) {
                throw new BadRequestException(
                                "Export is not supported for Query that requires traversing Relationships.");
            }
        }
    }
}
