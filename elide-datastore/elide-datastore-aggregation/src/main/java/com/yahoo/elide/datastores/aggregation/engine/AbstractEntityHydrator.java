package com.yahoo.elide.datastores.aggregation.engine;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.Query;
import com.yahoo.elide.datastores.aggregation.dimension.Dimension;
import com.yahoo.elide.datastores.aggregation.dimension.DimensionType;
import com.yahoo.elide.datastores.aggregation.engine.schema.SQLSchema;
import com.yahoo.elide.datastores.aggregation.metric.Metric;

import com.google.common.base.Preconditions;

import org.apache.commons.lang3.mutable.MutableInt;

import lombok.AccessLevel;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 */
public abstract class AbstractEntityHydrator {

    @Getter(AccessLevel.PROTECTED)
    private final EntityDictionary entityDictionary;

    @Getter(AccessLevel.PRIVATE)
    private final StitchList stitchList;

    @Getter(AccessLevel.PRIVATE)
    private final List<Object> results;

    @Getter(AccessLevel.PRIVATE)
    private final Query query;

    public AbstractEntityHydrator(List<Object> results, Query query, EntityDictionary entityDictionary) {
        this.stitchList = new StitchList(entityDictionary);
        this.results = new ArrayList<>(results);
        this.query = query;
        this.entityDictionary = entityDictionary;
    }

    /**
     * Returns a list of relationship objects whose ID matches a specified list of ID's
     * <p>
     * Note the relationship cannot be toMany. Regardless of ID duplicates, this method returns the list of relationship
     * objects in the same size as the specified list of ID's, i.e. duplicate ID mapps to the same object in the
     * returned list. For example:
     * <p>
     * if {@code entityClass = Country.java}, {@code joinField = country}, and {@code joinFieldIds = [840, 840, 344]},
     * then this method returns {@code Country(id:840), Country(id:840), Country(id:344)}.
     * <p>
     * If the ID list is empty or no matching objects are found, this method returns {@link Collections#emptyList()}.
     *
     * @param entityClass  The type of relationship
     * @param joinField  The relationship field name
     * @param joinFieldIds  The specified list of join ID's against the relationshiop
     *
     * @return a list of hydrating values
     */
    public abstract Map<Object, Object> getRelationshipValues(
            Class<?> entityClass,
            String joinField,
            List<Object> joinFieldIds
    );

    public Iterable<Object> hydrate() {
        //Coerce the results into entity objects.
        MutableInt counter = new MutableInt(0);

        List<Object> queryResults = getResults().stream()
                .map((result) -> { return result instanceof Object[] ? (Object []) result : new Object[] { result }; })
                .map((result) -> coerceObjectToEntity(result, counter))
                .collect(Collectors.toList());

        if (getStitchList().shouldStitch()) {
            // relationship is requested, stitch relationship then
            populateObjectLookupTable();
            getStitchList().stitch();
        }

        return queryResults;
    }

    /**
     * Coerces results from a {@link Query} into an Object.
     *
     * @param counter Monotonically increasing number to generate IDs.
     * @return A hydrated entity object.
     */
    protected Object coerceObjectToEntity(Object[] result, MutableInt counter) {
        Class<?> entityClass = query.getSchema().getEntityClass();

        //Get all the projections from the client query.
        List<String> projections = query.getMetrics().entrySet().stream()
                .map(Map.Entry::getKey)
                .map(Metric::getName)
                .collect(Collectors.toList());

        projections.addAll(query.getDimensions().stream()
                .map(Dimension::getName)
                .collect(Collectors.toList()));

        Preconditions.checkArgument(result.length == projections.size());

        SQLSchema schema = (SQLSchema) query.getSchema();

        //Construct the object.
        Object entityInstance;
        try {
            entityInstance = entityClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }

        //Populate all of the fields.
        for (int idx = 0; idx < result.length; idx++) {
            Object value = result[idx];
            String fieldName = projections.get(idx);

            Dimension dim = schema.getDimension(fieldName);
            if (dim != null && dim.getDimensionType() == DimensionType.ENTITY) {
                getStitchList().todo(entityInstance, fieldName, value);
                //We don't hydrate relationships here.
                continue;
            }

            getEntityDictionary().setValue(entityInstance, fieldName, value);
        }

        //Set the ID (it must be coerced from an integer)
        getEntityDictionary().setValue(
                entityInstance,
                getEntityDictionary().getIdFieldName(entityClass),
                counter.getAndIncrement()
        );

        return entityInstance;
    }

    private void populateObjectLookupTable() {
        // mapping: relationship field name -> join ID's
        Map<String, List<Object>> hydrationIdsByRelationship = getStitchList().getHydrationMapping();

        // hydrate each relationship
        for (Map.Entry<String, List<Object>> entry : hydrationIdsByRelationship.entrySet()) {
            String joinField = entry.getKey();
            List<Object> joinFieldIds = entry.getValue();
            Class<?> entityType = getEntityDictionary().getType(getQuery().getSchema().getEntityClass(), joinField);

            getStitchList().populateLookup(entityType, getRelationshipValues(entityType, joinField, joinFieldIds));
        }
    }
}
