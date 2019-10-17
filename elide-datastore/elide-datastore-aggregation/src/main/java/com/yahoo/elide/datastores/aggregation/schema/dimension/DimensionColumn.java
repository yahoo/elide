/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.schema.dimension;

import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.annotation.FriendlyName;
import com.yahoo.elide.datastores.aggregation.query.ProjectedDimension;

/**
 * Represents schema metadata that describes a dimension column.
 */
public interface DimensionColumn extends ProjectedDimension {
    /**
     * Returns a human-readable name (allowing spaces) of this {@link ProjectedDimension} object as a {@link String}.
     *
     * @return a human-readable name (allowing spaces) of this {@link ProjectedDimension}.
     */
    String getLongName();

    /**
     * Returns a short description explaining the meaning of this {@link ProjectedDimension}.
     *
     * @return dimension description
     */
    String getDescription();

    /**
     * Returns the type of this dimension.
     *
     * @return dimension type
     */
    DimensionType getDimensionType();

    /**
     * Returns the entity field type of this {@link ProjectedDimension}.
     *
     * @return dimension type
     */
    Class<?> getDataType();

    /**
     * Returns the estimated cardinality of this {@link ProjectedDimension}.
     *
     * @return a {@link CardinalitySize} reflecting the estimated cardinality
     */
    CardinalitySize getCardinality();

    /**
     * Returns the entity field decorated by {@link FriendlyName}.
     * <p>
     * An entity in aggregation data store must have a friendly name. It will fall into one of three
     * categories:
     * <ol>
     *     <li> A dimension table has a single attribute with the friendly name annotation. That attribute is the
     *          friendly name,
     *     <li> A dimension table has no attribute with the friendly name annotation. In this case {@literal @}Id field
     *          is the friendly name,
     *     <li> A fact table has a field as degenerate dimension. The friendly name is the attribute in that case,
     *          although there is no annotation on that attribute.
     * </ol>
     *
     * @return a human displayable name of this {@link ProjectedDimension}.
     */
    String getFriendlyName();
}
