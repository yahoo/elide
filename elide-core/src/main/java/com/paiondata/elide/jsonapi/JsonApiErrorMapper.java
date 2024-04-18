/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi;

import com.paiondata.elide.ElideError;
import com.paiondata.elide.jsonapi.models.JsonApiError;

/**
 * Maps {@link ElideError} to {@link JsonApiError}.
 */
public interface JsonApiErrorMapper {
    /**
     * Maps {@link ElideError} to {@link JsonApiError}.
     *
     * @param error the {@link ElideError} to map
     * @return the mapped {@link JsonApiError}
     */
    JsonApiError toJsonApiError(ElideError error);
}
