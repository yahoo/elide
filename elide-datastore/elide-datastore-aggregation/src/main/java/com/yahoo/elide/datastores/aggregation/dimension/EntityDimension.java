/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.dimension;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.Column;
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
public class EntityDimension extends Column implements Dimension {

    /**
     * Returns the entity field that is defined to be a human displayable column of that entity.
     *
     * @param cls  The entity or a relation
     * @param entityDictionary  A object that helps to find all fields with {@link FriendlyName} annotation
     *
     * @return friendlyName of the entity
     *
     * @throws IllegalStateException if more than 1 fields are annotated by the {@link FriendlyName}
     */
    public static String getFriendlyNameField(Class<?> cls, EntityDictionary entityDictionary) {
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
     * @param entityDictionary  An object that helps to retrieve {@link Cardinality} annotation from either entity or
     * field
     *
     * @return cardinality of the dimension field
     */
    public static CardinalitySize getEstimatedCardinality(
            String dimension,
            Class<?> cls,
            EntityDictionary entityDictionary
    ) {
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

    /**
     * The default cardinality for entity without {@link Cardinality} annotation.
     *
     * @return the default table size backing this {@link Dimension}
     */
    protected static CardinalitySize getDefaultCardinality() {
        return CardinalitySize.LARGE;
    }

    private static final long serialVersionUID = 4383532465610358102L;

    @Getter
    protected final DimensionType dimensionType;
    @Getter
    protected final CardinalitySize cardinality;
    @Getter
    protected final String friendlyName;

    /**
     * Constructor.
     *
     * @param dimensionField  The entity field or relation that this {@link Dimension} represents
     * @param annotation  Provides static meta data about this {@link Dimension}
     * @param fieldType  The Java type for this entity field or relation
     * @param cardinality  The estimated cardinality of this {@link Dimension} in SQL table
     * @param friendlyName  A human-readable name representing this {@link Dimension}
     *
     * @throws NullPointerException any argument, except for {@code annotation}, is {@code null}
     */
    public EntityDimension(
            String dimensionField,
            Meta annotation,
            Class<?> fieldType,
            CardinalitySize cardinality,
            String friendlyName
    ) {
        this(dimensionField, annotation, fieldType, DimensionType.TABLE, cardinality, friendlyName);
    }

    /**
     * Constructor.
     *
     * @param dimensionField  The entity field or relation that this {@link Dimension} represents
     * @param annotation  Provides static meta data about this {@link Dimension}
     * @param fieldType  The Java type for this entity field or relation
     * @param dimensionType  The physical storage structure backing this {@link Dimension}, such as a table or a column
     * @param cardinality  The estimated cardinality of this {@link Dimension} in SQL table
     * @param friendlyName  A human-readable name representing this {@link Dimension}
     *
     * @throws NullPointerException any argument, except for {@code annotation}, is {@code null}
     */
    protected EntityDimension(
            String dimensionField,
            Meta annotation,
            Class<?> fieldType,
            DimensionType dimensionType,
            CardinalitySize cardinality,
            String friendlyName
    ) {
        super(dimensionField, annotation, fieldType);

        this.dimensionType = Objects.requireNonNull(dimensionType, "dimensionType");
        this.cardinality = Objects.requireNonNull(cardinality, "cardinality");
        this.friendlyName = Objects.requireNonNull(friendlyName, "friendlyName");
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        if (!super.equals(other)) {
            return false;
        }

        final EntityDimension that = (EntityDimension) other;
        return getDimensionType() == that.getDimensionType()
                && getCardinality() == that.getCardinality()
                && getFriendlyName().equals(that.getFriendlyName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getDimensionType(), getCardinality(), getFriendlyName());
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
}
