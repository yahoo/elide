/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.dimension;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.annotation.Cardinality;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.annotation.FriendlyName;
import com.yahoo.elide.datastores.aggregation.annotation.Meta;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * A dimension backed by a table.
 * <p>
 * {@link EntityDimension} is thread-safe and can be accessed by multiple threads.
 */
@Slf4j
public class EntityDimension implements Dimension {

    private static final long serialVersionUID = 4383532465610358102L;

    @Getter
    protected final String name;

    @Getter
    protected final String longName;

    @Getter
    protected final String description;

    @Getter
    protected final DimensionType dimensionType;

    @Getter
    protected final Class<?> dataType;

    @Getter
    protected final CardinalitySize cardinality;

    @Getter
    protected final String friendlyName;

    public EntityDimension(String dimensionField, Class<?> cls, EntityDictionary entityDictionary) {
        this(dimensionField, cls, entityDictionary, DimensionType.TABLE);
    }

    protected EntityDimension(
            String dimensionField,
            Class<?> cls,
            EntityDictionary entityDictionary,
            DimensionType dimensionType
    ) {
        Meta metaData = entityDictionary.getAttributeOrRelationAnnotation(cls, Meta.class, dimensionField);
        Class<?> fieldType = entityDictionary.getType(cls, dimensionField);

        this.name = dimensionField;
        this.longName = metaData == null || metaData.longName().isEmpty() ? dimensionField : metaData.longName();
        this.description = metaData == null || metaData.description().isEmpty()
                ? dimensionField
                : metaData.description();
        this.dimensionType = dimensionType;
        this.dataType = fieldType;
        this.cardinality = getEstimatedCardinality(dimensionField, cls, entityDictionary);
        this.friendlyName = getFriendlyNameField(cls, entityDictionary);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        final EntityDimension that = (EntityDimension) other;
        return getName().equals(that.getName())
                && getLongName().equals(that.getLongName())
                && getDescription().equals(that.getDescription())
                && getDimensionType().equals(that.getDimensionType())
                && getDataType().equals(that.getDataType())
                && getCardinality() == that.getCardinality()
                && getFriendlyName().equals(that.getFriendlyName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getName(),
                getLongName(),
                getDescription(),
                getDimensionType(),
                getDataType(),
                getCardinality(),
                getFriendlyName()
        );
    }

    /**
     * Returns the string representation of this {@link Dimension}.
     * <p>
     * The string consists of values of all fields in the format
     * "EntityDimension[name='XXX', longName='XXX', description='XXX', dimensionType=XXX, dataType=XXX, cardinality=XXX,
     * friendlyName=XXX]", where values can be programmatically fetched via getters.
     * <p>
     * {@code dataType} are printed in its {@link Class#getSimpleName() simple name}.
     * <p>
     * Note that there is a single space separating each value pair.
     *
     * @return serialized {@link EntityDimension}
     */
    @Override
    public String toString() {
        return new StringJoiner(", ", EntityDimension.class.getSimpleName() + "[", "]")
                .add("name='" + getName() + "'")
                .add("longName='" + getLongName() + "'")
                .add("description='" + getDescription() + "'")
                .add("dimensionType=" + getDimensionType())
                .add("dataType=" + getDataType().getSimpleName())
                .add("cardinality=" + getCardinality())
                .add("friendlyName='" + getFriendlyName() + "'")
                .toString();
    }

    /**
     * The default cardinality for entity without {@link Cardinality} annotation.
     *
     * @return the default table size backing this {@link Dimension}
     */
    protected static CardinalitySize getDefaultCardinality() {
        return CardinalitySize.LARGE;
    }

    /**
     * Returns the entity field that is defined to be a human displayable column of that entity.
     *
     * @param cls  The entity or a relation
     *
     * @return friendlyName of the entity
     *
     * @throws IllegalStateException if more than 1 fields are annotated by the {@link FriendlyName}
     */
    private String getFriendlyNameField(Class<?> cls, EntityDictionary entityDictionary) {
        List<String> singleFriendlyName = entityDictionary.getAllFields(cls).stream()
                .filter(field -> entityDictionary.attributeOrRelationAnnotationExists(
                        cls,
                        field,
                        FriendlyName.class
                ))
                .collect(Collectors.toList());

        if (singleFriendlyName.size() > 1) {
            String message = String.format(
                    "Multiple @FriendlyName fields found in entity '%s'. Can only have 0 or 1",
                    cls.getName()
            );
            log.error(message);
            throw new IllegalStateException(message);
        }

        return singleFriendlyName.isEmpty()
                ? entityDictionary.getIdFieldName(cls) // no friendly name found; use @Id field as friendly name
                : singleFriendlyName.get(0);
    }

    /**
     * Returns the estimated cardinality of this a dimension field.
     * <p>
     * {@link #getDefaultCardinality() Default} is returned when the dimension is not annotated by {@link Cardinality}.
     *
     * @param dimension  The dimension field
     * @param cls  The entity name of the dimension from which estimated cardinality is being looked up
     *
     * @return cardinality of the dimension field
     */
    private CardinalitySize getEstimatedCardinality(String dimension, Class<?> cls, EntityDictionary entityDictionary) {
        if (entityDictionary.isRelation(cls, dimension)) {
            // try to get annotation from entity first
            Cardinality annotation = entityDictionary.getAttributeOrRelationAnnotation(
                    cls,
                    Cardinality.class,
                    dimension
            );

            if (annotation != null) {
                return annotation.size();
            }

            // annotation is not on entity; then must be on field or method
        }

        Cardinality annotation = entityDictionary
                .getAttributeOrRelationAnnotation(cls, Cardinality.class, dimension);

        return annotation == null
                ? getDefaultCardinality() // no cardinality specified on field or method; use default then
                : annotation.size();
    }
}
