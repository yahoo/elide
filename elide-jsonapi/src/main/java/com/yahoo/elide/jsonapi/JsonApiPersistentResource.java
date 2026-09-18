/*
 * Copyright 2025, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi;

import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.dictionary.RelationshipType;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.core.exceptions.InvalidEntityBodyException;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.jsonapi.document.processors.WithMetadata;
import com.yahoo.elide.jsonapi.models.Data;
import com.yahoo.elide.jsonapi.models.Meta;
import com.yahoo.elide.jsonapi.models.Relationship;
import com.yahoo.elide.jsonapi.models.Resource;
import com.yahoo.elide.jsonapi.models.ResourceIdentifier;

import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Converts a {@link PersistentResource} into a JSON-API {@link Resource}.
 */
public final class JsonApiPersistentResource {

    /* Sort strings first by length then contents */
    private static final Comparator<String> LENGTH_FIRST_COMPARATOR = (string1, string2) -> {
        int diff = string1.length() - string2.length();
        return diff == 0 ? string1.compareTo(string2) : diff;
    };

    private JsonApiPersistentResource() {
        // Utility class
    }

    /**
     * Convert a persistent resource to a resource.
     *
     * @param persistentResource the persistent resource
     * @return a resource
     */
    public static Resource toResource(PersistentResource<?> persistentResource) {
        return toResource(persistentResource, getRelationships(persistentResource),
                getAttributes(persistentResource));
    }

    /**
     * Convert a persistent resource to a resource, scoped to the given projection.
     *
     * @param persistentResource the persistent resource
     * @param projection         the projection
     * @return the resource
     */
    public static Resource toResource(PersistentResource<?> persistentResource, EntityProjection projection) {
        return toResource(persistentResource, getRelationships(persistentResource, projection),
                getAttributes(persistentResource));
    }

    /**
     * Convert a persistent resource to a resource.
     *
     * @param persistentResource the persistent resource
     * @param relationships      the relationships
     * @param attributes         the attributes
     * @return the resource
     */
    public static Resource toResource(PersistentResource<?> persistentResource,
            Map<String, Relationship> relationships, Map<String, Object> attributes) {
        Object obj = persistentResource.getObject();
        final Resource resource = new Resource(persistentResource.getTypeName(), (obj == null)
                ? persistentResource.getUUID().orElseThrow(
                        () -> new InvalidEntityBodyException("No id found on object"))
                : persistentResource.getDictionary().getId(obj));
        resource.setRelationships(relationships);
        resource.setAttributes(attributes);

        JsonApiSettings jsonApiSettings = persistentResource.getRequestScope().getElideSettings()
                .getSettings(JsonApiSettings.class);
        if (jsonApiSettings != null && jsonApiSettings.getLinks().isEnabled()) {
            resource.setLinks(jsonApiSettings.getLinks().getJsonApiLinks().getResourceLevelLinks(persistentResource));
        }

        if (!(obj instanceof WithMetadata)) {
            return resource;
        }

        WithMetadata withMetadata = (WithMetadata) obj;
        Set<String> fields = withMetadata.getMetadataFields();

        if (fields.isEmpty()) {
            return resource;
        }

        Meta meta = new Meta(new LinkedHashMap<>());

        for (String field : fields) {
            meta.getMetaMap().put(field, withMetadata.getMetadataField(field).get());
        }

        resource.setMeta(meta);

        return resource;
    }

    /**
     * Get relationship mappings.
     *
     * @param persistentResource the persistent resource
     * @return Relationship mapping
     */
    public static Map<String, Relationship> getRelationships(PersistentResource<?> persistentResource) {
        return getRelationshipsWithRelationshipFunction(persistentResource, (relationName) -> {
            RequestScope requestScope = persistentResource.getRequestScope();
            Optional<FilterExpression> filterExpression = (requestScope instanceof JsonApiRequestScope jsonApiScope)
                    ? jsonApiScope.getExpressionForRelation(persistentResource.getResourceType(), relationName)
                    : Optional.empty();

            return persistentResource.getRelationCheckedFiltered(com.yahoo.elide.core.request.Relationship.builder()
                    .alias(relationName)
                    .name(relationName)
                    .projection(EntityProjection.builder()
                            .type(persistentResource.getDictionary()
                                    .getParameterizedType(persistentResource.getResourceType(), relationName))
                            .filterExpression(filterExpression.orElse(null))
                            .build())
                    .build());
        });
    }

    /**
     * Get relationship mappings, scoped to the given projection.
     *
     * @param persistentResource the persistent resource
     * @param projection         the projection
     * @return Relationship mapping
     */
    public static Map<String, Relationship> getRelationships(PersistentResource<?> persistentResource,
            EntityProjection projection) {
        return getRelationshipsWithRelationshipFunction(persistentResource,
                (relationName) -> persistentResource.getRelationCheckedFiltered(
                        projection.getRelationship(relationName).orElseThrow(IllegalStateException::new)));
    }

    /**
     * Get relationship mappings.
     *
     * @param persistentResource   the persistent resource
     * @param relationshipFunction a function to load the value of a relationship. Takes a string of the relationship
     *                             name and returns the relationship's value.
     * @return Relationship mapping
     */
    private static Map<String, Relationship> getRelationshipsWithRelationshipFunction(
            PersistentResource<?> persistentResource,
            final Function<String, Flux<PersistentResource>> relationshipFunction) {
        final Map<String, Relationship> relationshipMap = new LinkedHashMap<>();
        final Set<String> relationshipFields = filterFields(persistentResource,
                persistentResource.getDictionary().getRelationships(persistentResource.getObject()));

        for (String field : relationshipFields) {
            TreeMap<String, Resource> orderedById = new TreeMap<>(LENGTH_FIRST_COMPARATOR);
            for (PersistentResource relationship : relationshipFunction.apply(field).collectList().block()) {
                orderedById.put(relationship.getId(),
                        new ResourceIdentifier(relationship.getTypeName(), relationship.getId()).castToResource());

            }
            Flux<Resource> resources = Flux.fromIterable(orderedById.values());

            Data<Resource> data;
            RelationshipType relationshipType = persistentResource.getRelationshipType(field);
            if (relationshipType.isToOne()) {
                data = new Data<>(PersistentResource.firstOrNullIfEmpty(resources));
            } else {
                data = new Data<>(resources);
            }
            Map<String, String> links = null;
            JsonApiSettings jsonApiSettings = persistentResource.getRequestScope().getElideSettings()
                    .getSettings(JsonApiSettings.class);
            if (jsonApiSettings != null && jsonApiSettings.getLinks().isEnabled()) {
                links = jsonApiSettings.getLinks().getJsonApiLinks().getRelationshipLinks(persistentResource, field);
            }
            relationshipMap.put(field, new Relationship(links, data));
        }

        return relationshipMap;
    }

    /**
     * Get attributes mapping from entity.
     *
     * @param persistentResource the persistent resource
     * @return Mapping of attributes to objects
     */
    public static Map<String, Object> getAttributes(PersistentResource<?> persistentResource) {
        final Map<String, Object> attributes = new LinkedHashMap<>();

        final Set<String> attrFields = filterFields(persistentResource,
                persistentResource.getDictionary().getAttributes(persistentResource.getObject()));
        for (String field : attrFields) {
            Object val = persistentResource.getAttribute(field);
            attributes.put(field, val);
        }
        return attributes;
    }

    /**
     * Filter a set of fields to those that are sparse-field requested (if any) and readable.
     *
     * @param persistentResource the persistent resource
     * @param fields             the fields
     * @return Filtered set of fields
     */
    private static Set<String> filterFields(PersistentResource<?> persistentResource, Collection<String> fields) {
        Set<String> filteredSet = new LinkedHashSet<>();
        RequestScope requestScope = persistentResource.getRequestScope();
        Map<String, Set<String>> sparseFields = (requestScope instanceof JsonApiRequestScope jsonApiScope)
                ? jsonApiScope.getSparseFields()
                : Map.of();
        Stream<String> stream;
        if (sparseFields.isEmpty()) {
            // If sparse fields is not set return all fields
            stream = fields.stream();
        } else {
            // If sparse fields is set return those fields
            Set<String> byType = sparseFields.get(persistentResource.getTypeName());
            if (byType == null || fields == null || byType.isEmpty() || fields.isEmpty()) {
                stream = Stream.empty();
            } else {
                stream = byType.stream().filter(fields::contains);
            }
        }
        stream.forEach(field -> {
            try {
                persistentResource.getRequestScope().getPermissionExecutor()
                        .checkSpecificFieldPermissions(persistentResource, null, ReadPermission.class, field);
                filteredSet.add(field);
            } catch (ForbiddenAccessException e) {
                // Do nothing. Filter from set.
            }
        });
        return filteredSet;
    }
}
