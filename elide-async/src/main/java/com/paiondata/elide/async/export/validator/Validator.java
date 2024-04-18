/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.export.validator;

import com.paiondata.elide.core.request.EntityProjection;

import java.util.Collection;

/**
 * Utility interface used to validate Entity Projections.
 */
public interface Validator {

    /**
     * Validates the EntityProjection.
     * @param projections Collection of EntityProjections to validate.
     */
    public void validateProjection(Collection<EntityProjection> projections);
}
