/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.swagger;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.contrib.swagger.model.Resource;
import com.yahoo.elide.contrib.swagger.property.Relationship;
import com.yahoo.elide.core.EntityDictionary;

import com.fasterxml.jackson.databind.type.SimpleType;

import io.swagger.converter.ModelConverter;
import io.swagger.converter.ModelConverterContext;
import io.swagger.jackson.ModelResolver;
import io.swagger.models.Model;
import io.swagger.models.properties.Property;
import io.swagger.util.Json;

import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;

/**
 * Swagger ModelResolvers map POJO classes to Swagger com.yahoo.elide.contrib.swagger.models.
 * This resolver maps the POJO to a JSON-API Resource.
 */
public class JsonApiModelResolver extends ModelResolver {
    private final EntityDictionary dictionary;

    public JsonApiModelResolver(EntityDictionary dictionary) {
        super(Json.mapper());
        this.dictionary = dictionary;
    }

    @Override
    public Model resolve(Type type, ModelConverterContext context, Iterator<ModelConverter> next) {
        /*
         * If an Elide entity is an attribute somewhere in a model, the ModelResolver will
         * end up wrapping this as a SimpleType (rather than trying to resolve the entity class directly).
         */
        if (type instanceof SimpleType) {
            type = ((SimpleType) type).getRawClass();
        }

        if (!(type instanceof Class)) {
            return super.resolve(type, context, next);
        }

        Class<?> clazz = (Class<?>) type;

        /* Not an entity managed by Elide, let Swagger convert it */
        String typeAlias;
        try {
            typeAlias = dictionary.getJsonAliasFor(clazz);
        } catch (IllegalArgumentException e) {
            return super.resolve(type, context, next);
        }

        Resource entitySchema = new Resource();
        entitySchema.setSecurityDescription(getClassPermissions(clazz));

        /* Populate the attributes */
        List<String> attributeNames = dictionary.getAttributes(clazz);
        for (String attributeName : attributeNames) {
            Class<?> attributeClazz = dictionary.getType(clazz, attributeName);

            Property attribute = super.resolveProperty(attributeClazz, context, null, next);

            attribute.setDescription(getFieldPermissions(clazz, attributeName));
            entitySchema.addAttribute(attributeName, attribute);
        }

        /* Populate the relationships */
        List<String> relationshipNames = dictionary.getRelationships(clazz);
        for (String relationshipName : relationshipNames) {

            Class<?> relationshipType = dictionary.getParameterizedType(clazz, relationshipName);
            Relationship relationship;
            try {
                relationship = new Relationship(dictionary.getJsonAliasFor(relationshipType));

            /* Skip the relationship if it is not bound in the dictionary */
            } catch (IllegalArgumentException e) {
                continue;
            }
            relationship.setDescription(getFieldPermissions(clazz, relationshipName));

            entitySchema.addRelationship(relationshipName, relationship);
        }

        entitySchema.name(typeAlias);
        return entitySchema;
    }

    /**
     * Get the class-level permission annotation value.
     *
     * @param clazz the entity class
     * @return the create and delete permissions for the entity class.
     */
    protected String getClassPermissions(Class<?> clazz) {
        String createPermissions = getCreatePermission(clazz);
        String deletePermissions = getDeletePermission(clazz);


        createPermissions = (createPermissions == null) ? "" : "Create Permissions : (" + createPermissions + ")\n";
        deletePermissions = (deletePermissions == null) ? "" : "Delete Permissions : (" + deletePermissions + ")\n";
        return createPermissions + deletePermissions;
    }

    /**
     * Get the field level permission annotation value.
     *
     * @param clazz the entity class
     * @param fieldName the field
     * @return read and update permissions for a field.
     */
    protected String getFieldPermissions(Class<?> clazz, String fieldName) {
        String readPermissions = getReadPermission(clazz, fieldName);
        String updatePermissions = getUpdatePermission(clazz, fieldName);

        readPermissions = (readPermissions == null) ? "" : "Read Permissions : (" + readPermissions + ")\n";
        updatePermissions = (updatePermissions == null) ? "" : "Update Permissions : (" + updatePermissions + ")\n";
        return readPermissions + updatePermissions;
    }

    /**
     * Get the calculated {@link ReadPermission} value for the field.
     *
     * @param clazz the entity class
     * @param fieldName the field
     * @return the read permissions for a field
     */
    protected String getReadPermission(Class<?> clazz, String fieldName) {
        ReadPermission classPermission = dictionary.getAnnotation(clazz, ReadPermission.class);
        ReadPermission fieldPermission = dictionary.getAttributeOrRelationAnnotation(clazz, ReadPermission.class,
                fieldName);

        if (fieldPermission != null) {
            return fieldPermission.expression();
        }
        if (classPermission != null) {
            return classPermission.expression();
        }
        return null;
    }

    /**
     * Get the calculated {@link UpdatePermission} value for the field.
     *
     * @param clazz the entity class
     * @param fieldName the field
     * @return the update permissions for a field
     */
    protected String getUpdatePermission(Class<?> clazz, String fieldName) {
        UpdatePermission classPermission = dictionary.getAnnotation(clazz, UpdatePermission.class);
        UpdatePermission fieldPermission = dictionary.getAttributeOrRelationAnnotation(clazz, UpdatePermission.class,
                fieldName);

        if (fieldPermission != null) {
            return fieldPermission.expression();
        }
        if (classPermission != null) {
            return classPermission.expression();
        }
        return null;
    }

    /**
     * Get the calculated {@link CreatePermission} value for the field.
     *
     * @param clazz the entity class
     * @return the create permissions for an entity
     */
    protected String getCreatePermission(Class<?> clazz) {
        CreatePermission classPermission = dictionary.getAnnotation(clazz, CreatePermission.class);

        if (classPermission != null) {
            return classPermission.expression();
        }
        return null;
    }

    /**
     * Get the calculated {@link DeletePermission} value for the field.
     *
     * @param clazz the entity class
     * @return the delete permissions for an entity
     */
    protected String getDeletePermission(Class<?> clazz) {
        DeletePermission classPermission = dictionary.getAnnotation(clazz, DeletePermission.class);

        if (classPermission != null) {
            return classPermission.expression();
        }
        return null;
    }
}
