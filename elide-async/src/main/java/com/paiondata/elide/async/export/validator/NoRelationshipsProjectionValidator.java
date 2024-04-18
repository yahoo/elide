/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.export.validator;

import com.paiondata.elide.core.exceptions.BadRequestException;
import com.paiondata.elide.core.request.EntityProjection;

import java.util.Collection;

/**
 * Validates none of the projections have relationships.
 */
public class NoRelationshipsProjectionValidator implements Validator {

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
