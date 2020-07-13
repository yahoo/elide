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
import org.apache.commons.lang3.StringUtils;

import io.swagger.annotations.ApiModelProperty;
import io.swagger.converter.ModelConverter;
import io.swagger.converter.ModelConverterContext;
import io.swagger.jackson.ModelResolver;
import io.swagger.models.Model;
import io.swagger.models.properties.Property;
import io.swagger.util.Json;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

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

            Property attribute = processAttribute(clazz, attributeName, attributeClazz, context, next);
            entitySchema.addAttribute(attributeName, attribute);
        }

        /* Populate the relationships */
        List<String> relationshipNames = dictionary.getRelationships(clazz);
        for (String relationshipName : relationshipNames) {

            Class<?> relationshipType = dictionary.getParameterizedType(clazz, relationshipName);

            Relationship relationship = processRelationship(clazz, relationshipName, relationshipType);

            if (relationship != null) {
                entitySchema.addRelationship(relationshipName, relationship);
            }

        }

        entitySchema.name(typeAlias);
        return entitySchema;
    }

    private Property processAttribute(Class<?> clazz, String attributeName, Class<?> attributeClazz,
        ModelConverterContext context, Iterator<ModelConverter> next) {

        Property attribute = super.resolveProperty(attributeClazz, context, null, next);

        String permissions = getFieldPermissions(clazz, attributeName);
        String description = getFieldDescription(clazz, attributeName);

        attribute.setDescription(StringUtils.defaultIfEmpty(joinNonEmpty("\n", description, permissions), null));
        attribute.setExample((Object) StringUtils.defaultIfEmpty(getFieldExample(clazz, attributeName), null));
        attribute.setReadOnly(getFieldReadOnly(clazz, attributeName));
        attribute.setRequired(getFieldRequired(clazz, attributeName));

        return attribute;
    }

    private Relationship processRelationship(Class<?> clazz, String relationshipName, Class<?> relationshipClazz) {
        Relationship relationship = null;
        try {
            relationship = new Relationship(dictionary.getJsonAliasFor(relationshipClazz));

        /* Skip the relationship if it is not bound in the dictionary */
        } catch (IllegalArgumentException e) {
            return relationship;
        }

        String description = getFieldDescription(clazz, relationshipName);
        String permissions = getFieldPermissions(clazz, relationshipName);

        relationship.setDescription(StringUtils.defaultIfEmpty(joinNonEmpty("\n", description, permissions), null));
        relationship.setExample((Object) StringUtils.defaultIfEmpty(getFieldExample(clazz, relationshipName), null));
        relationship.setReadOnly(getFieldReadOnly(clazz, relationshipName));
        relationship.setRequired(getFieldRequired(clazz, relationshipName));

        return relationship;
    }

    private ApiModelProperty getApiModelProperty(Class<?> clazz, String fieldName) {
        return dictionary.getAttributeOrRelationAnnotation(clazz, ApiModelProperty.class, fieldName);
    }

    private boolean getFieldRequired(Class<?> clazz, String fieldName) {
        ApiModelProperty property = getApiModelProperty(clazz, fieldName);
        return property != null && property.required();
    }

    private boolean getFieldReadOnly(Class<?> clazz, String fieldName) {
        ApiModelProperty property = getApiModelProperty(clazz, fieldName);
        return property != null && property.readOnly();
    }

    private String getFieldExample(Class<?> clazz, String fieldName) {
        ApiModelProperty property = getApiModelProperty(clazz, fieldName);
        return property == null ? "" : property.example();
    }

    private String getFieldDescription(Class<?> clazz, String fieldName) {
        ApiModelProperty property = getApiModelProperty(clazz, fieldName);
        return property == null ? "" : property.value();
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

        createPermissions = (createPermissions == null) ? "" : "Create Permissions : (" + createPermissions + ")";
        deletePermissions = (deletePermissions == null) ? "" : "Delete Permissions : (" + deletePermissions + ")";
        return joinNonEmpty("\n", createPermissions, deletePermissions);
    }

    private String joinNonEmpty(String delimiter, String... elements) {
        return Arrays.stream(elements).filter(StringUtils::isNotBlank)
            .collect(Collectors.joining(delimiter));
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

        readPermissions = (readPermissions == null) ? "" : "Read Permissions : (" + readPermissions + ")";
        updatePermissions = (updatePermissions == null) ? "" : "Update Permissions : (" + updatePermissions + ")";
        return joinNonEmpty("\n", readPermissions, updatePermissions);
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
