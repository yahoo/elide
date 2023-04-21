/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.swagger.converter;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.swagger.models.media.Relationship;
import com.yahoo.elide.swagger.models.media.Resource;
import com.fasterxml.jackson.databind.type.SimpleType;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.annotations.media.Schema.AccessMode;
import io.swagger.v3.oas.models.media.Schema;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Swagger ModelResolvers map POJO classes to Swagger example.models.
 * This resolver maps the POJO to a JSON-API Resource.
 */
public class JsonApiModelResolver extends ModelResolver {
    private final EntityDictionary dictionary;

    public JsonApiModelResolver(EntityDictionary dictionary) {
        super(Json.mapper());
        this.dictionary = dictionary;
    }

    @Override
    public Schema<?> resolve(AnnotatedType annotatedType, ModelConverterContext context,
            Iterator<ModelConverter> next) {
        java.lang.reflect.Type type = annotatedType.getType();
        if (!(type instanceof Class || type instanceof SimpleType || type instanceof Type)) {
            return super.resolve(annotatedType, context, next);
        }

        Type<?> clazzType = null;

        /*
         * If an Elide entity is an attribute somewhere in a model, the ModelResolver will
         * end up wrapping this as a SimpleType (rather than trying to resolve the entity class directly).
         */
        if (type instanceof SimpleType) {
            type = ((SimpleType) type).getRawClass();
            clazzType = ClassType.of((Class<?>) type);
        } else if (type instanceof Type) {
            clazzType = (Type<?>) type;
        } else if (type instanceof Class) {
            clazzType = ClassType.of((Class<?>) type);
        }

        /* Not an entity managed by Elide, let Swagger convert it */
        String typeAlias;
        try {
            typeAlias = dictionary.getJsonAliasFor(clazzType);
        } catch (IllegalArgumentException e) {
            return super.resolve(annotatedType, context, next);
        }

        Resource entitySchema = new Resource();
        entitySchema.description(getModelDescription(clazzType));
        entitySchema.setSecurityDescription(getClassPermissions(clazzType));

        /* Populate the attributes */
        List<String> attributeNames = dictionary.getAttributes(clazzType);
        for (String attributeName : attributeNames) {
            Type<?> attributeType = dictionary.getType(clazzType, attributeName);

            Schema<?> attribute = processAttribute(clazzType, attributeName, attributeType,
                            context, next);
            entitySchema.addAttribute(attributeName, attribute);
        }

        /* Populate the relationships */
        List<String> relationshipNames = dictionary.getRelationships(clazzType);
        for (String relationshipName : relationshipNames) {

            Type<?> relationshipType = dictionary.getParameterizedType(clazzType, relationshipName);

            Relationship relationship = processRelationship(clazzType, relationshipName, relationshipType);

            if (relationship != null) {
                entitySchema.addRelationship(relationshipName, relationship);
            }

        }

        entitySchema.name(typeAlias);
        return entitySchema;
    }

    @SuppressWarnings("rawtypes")
    private Schema<?> processAttribute(Type<?> clazzType, String attributeName, Type<?> attributeType,
        ModelConverterContext context, Iterator<ModelConverter> next) {

        Preconditions.checkState(attributeType instanceof ClassType);
        Class<?> attributeTypeClass = ((ClassType) attributeType).getCls();
        Schema<?> attribute = super.resolve(new AnnotatedType().type(attributeTypeClass), context, next);
        if (attribute == null) {
            attribute = super.resolve(new AnnotatedType().resolveAsRef(true).type(attributeTypeClass), context, next);
        }
        String permissions = getFieldPermissions(clazzType, attributeName);
        String description = getFieldDescription(clazzType, attributeName);

        attribute.setDescription(StringUtils.defaultIfEmpty(joinNonEmpty("\n", description, permissions), null));
        attribute.setExample(StringUtils.defaultIfEmpty(getFieldExample(clazzType, attributeName), null));
        attribute.setReadOnly(getFieldReadOnly(clazzType, attributeName));
        attribute.setRequired(getFieldRequired(clazzType, attributeName));

        return attribute;
    }

    private Relationship processRelationship(Type<?> clazz, String relationshipName, Type<?> relationshipClazz) {
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
        relationship.setExample(StringUtils.defaultIfEmpty(getFieldExample(clazz, relationshipName), null));
        relationship.setReadOnly(getFieldReadOnly(clazz, relationshipName));
        relationship.setRequired(getFieldRequired(clazz, relationshipName));

        return relationship;
    }

    private io.swagger.v3.oas.annotations.media.Schema getSchema(Type<?> clazz) {
        return dictionary.getAnnotation(clazz, io.swagger.v3.oas.annotations.media.Schema.class);
    }

    private String getModelDescription(Type<?> clazz) {
        io.swagger.v3.oas.annotations.media.Schema model = getSchema(clazz);
        if (model == null) {

            String description = EntityDictionary.getEntityDescription(clazz);

            if (StringUtils.isEmpty(description)) {
                return null;
            }

            return description;
        }
        return model.description();
    }

    private io.swagger.v3.oas.annotations.media.Schema getSchema(Type<?> clazz, String fieldName) {
        return dictionary.getAttributeOrRelationAnnotation(clazz, io.swagger.v3.oas.annotations.media.Schema.class,
                fieldName);
    }

    private List<String> getFieldRequired(Type<?> clazz, String fieldName) {
        io.swagger.v3.oas.annotations.media.Schema property = getSchema(clazz, fieldName);
        return property != null ? Arrays.asList(property.requiredProperties()) : Collections.emptyList();
    }

    private boolean getFieldReadOnly(Type<?> clazz, String fieldName) {
        io.swagger.v3.oas.annotations.media.Schema property = getSchema(clazz, fieldName);
        return property != null && AccessMode.READ_ONLY.equals(property.accessMode());
    }

    private String getFieldExample(Type<?> clazz, String fieldName) {
        io.swagger.v3.oas.annotations.media.Schema property = getSchema(clazz, fieldName);
        return property == null ? "" : property.example();
    }

    private String getFieldDescription(Type<?> clazz, String fieldName) {
        io.swagger.v3.oas.annotations.media.Schema property = getSchema(clazz, fieldName);
        return property == null ? "" : property.description();
    }

    /**
     * Get the class-level permission annotation value.
     *
     * @param clazz the entity class
     * @return the create and delete permissions for the entity class.
     */
    protected String getClassPermissions(Type<?> clazz) {
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
    protected String getFieldPermissions(Type<?> clazz, String fieldName) {
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
    protected String getReadPermission(Type<?> clazz, String fieldName) {
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
    protected String getUpdatePermission(Type<?> clazz, String fieldName) {
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
    protected String getCreatePermission(Type<?> clazz) {
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
    protected String getDeletePermission(Type<?> clazz) {
        DeletePermission classPermission = dictionary.getAnnotation(clazz, DeletePermission.class);

        if (classPermission != null) {
            return classPermission.expression();
        }
        return null;
    }
}
