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
 * Validates each projection in collection have one projection only.
 */
public class SingleRootProjectionValidator implements Validator {

    @Override
    public void validateProjection(Collection<EntityProjection> projections) {
        if (projections.size() != 1) {
            throw new BadRequestException("Export is only supported for single Query with one root projection.");
        }
    }
}
