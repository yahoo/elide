/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.swagger.converter;

import com.paiondata.elide.annotation.CreatePermission;
import com.paiondata.elide.annotation.DeletePermission;
import com.paiondata.elide.annotation.Include;
import com.paiondata.elide.annotation.ReadPermission;
import com.paiondata.elide.annotation.UpdatePermission;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.type.ClassType;
import com.paiondata.elide.core.type.Type;
import com.paiondata.elide.core.utils.coerce.converters.Serde;
import com.paiondata.elide.swagger.models.media.Relationship;
import com.paiondata.elide.swagger.models.media.Resource;
import com.fasterxml.jackson.databind.type.SimpleType;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.annotations.media.Schema.AccessMode;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import io.swagger.v3.oas.models.media.Schema;

import java.lang.reflect.Method;
import java.util.ArrayList;
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
        if (!dictionary.hasBinding(clazzType)
                || !dictionary.getEntityBinding(clazzType).isElideModel()) {
            return super.resolve(annotatedType, context, next);
        }

        return getEntitySchema(clazzType, context, next);
    }

    private Resource getEntitySchema(final Type<?> clazzType,
                                     final ModelConverterContext context,
                                     final Iterator<ModelConverter> next) {
        Resource entitySchema = new Resource();
        entitySchema.name(dictionary.getJsonAliasFor(clazzType));
        entitySchema.description(getSchemaDescription(clazzType));
        entitySchema.setSecurityDescription(getClassPermissions(clazzType));

        Include include = getInclude(clazzType);
        if (include != null && !StringUtils.isBlank(include.friendlyName())) {
            entitySchema.setTitle(include.friendlyName());
        }
        io.swagger.v3.oas.annotations.media.Schema schema = getSchema(clazzType);
        if (schema != null && !StringUtils.isBlank(schema.title())) {
            entitySchema.setTitle(schema.title());
        }

        /* Populate */
        populateAttributes(entitySchema, clazzType, context, next);
        populateRelationships(entitySchema, clazzType);

        return entitySchema;
    }
    private void populateAttributes(final Resource entitySchema, final Type<?> clazzType,
                                    final ModelConverterContext context,
                                    final Iterator<ModelConverter> next) {
        List<String> requiredAttributes = new ArrayList<>();
        List<String> attributeNames = dictionary.getAttributes(clazzType);
        for (String attributeName : attributeNames) {
            Type<?> attributeType = dictionary.getType(clazzType, attributeName);

            Schema<?> attribute = processAttribute(clazzType, attributeName, attributeType,
                    context, next, requiredAttributes);
            entitySchema.addAttribute(attributeName, attribute);
        }
        if (!requiredAttributes.isEmpty()) {
            entitySchema.getAttributes().required(requiredAttributes);
        }
    }

    private void populateRelationships(final Resource entitySchema, final Type<?> clazzType) {
        List<String> requiredRelationships = new ArrayList<>();
        List<String> relationshipNames = dictionary.getRelationships(clazzType);
        for (String relationshipName : relationshipNames) {

            Type<?> relationshipType = dictionary.getParameterizedType(clazzType, relationshipName);

            Relationship relationship = processRelationship(clazzType, relationshipName, relationshipType,
                    requiredRelationships);

            if (relationship != null) {
                entitySchema.addRelationship(relationshipName, relationship);
            }
        }
        if (!requiredRelationships.isEmpty()) {
            entitySchema.getRelationships().required(requiredRelationships);
        }

        entitySchema.name(getSchemaName(clazzType));

        Include include = getInclude(clazzType);
        if (include != null) {
            if (!StringUtils.isBlank(include.friendlyName())) {
                entitySchema.setTitle(include.friendlyName());
            }
        }
        io.swagger.v3.oas.annotations.media.Schema schema = getSchema(clazzType);
        if (schema != null) {
            if (!StringUtils.isBlank(schema.title())) {
                entitySchema.setTitle(schema.title());
            }
        }
    }

    protected String getSchemaName(Type<?> type) {
        String schemaName = dictionary.getJsonAliasFor(type);
        String apiVersion = EntityDictionary.getModelVersion(type);
        if (!EntityDictionary.NO_VERSION.equals(apiVersion)) {
            schemaName = "v" + apiVersion + "_" + schemaName;
        }
        return schemaName;
    }

    @SuppressWarnings("rawtypes")
    private Class<?> getSerdeSerializedClass(Serde serde) {
        // Gets the serde interface type argument
        Class<?> attributeTypeClass = Object.class;

        try {
            for (java.lang.reflect.Type type : serde.getClass().getGenericInterfaces()) {
                if (type instanceof java.lang.reflect.ParameterizedType parameterizedType) {
                    if (Serde.class.equals(parameterizedType.getRawType())) {
                        attributeTypeClass = (Class<?>) parameterizedType.getActualTypeArguments()[0];
                    }
                }
            }
        } catch (RuntimeException e) {
            // Do nothing
        }

        // Using Object.class as the type argument isn't very helpful so try to get the return type
        try {
            if (Object.class.equals(attributeTypeClass)) {
                for (Method method : serde.getClass().getDeclaredMethods()) {
                    if ("serialize".equals(method.getName())) {
                        Class<?> returnType = method.getReturnType();
                        if (!Object.class.equals(returnType)) {
                           return returnType;
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            // Do nothing
        }
        return attributeTypeClass;
    }

    @SuppressWarnings("rawtypes")
    private Schema<?> processAttribute(Type<?> clazzType, String attributeName, Type<?> attributeType,
        ModelConverterContext context, Iterator<ModelConverter> next, List<String> required) {

        Preconditions.checkState(attributeType instanceof ClassType);
        Class<?> attributeTypeClass = ((ClassType) attributeType).getCls();

        Serde serde = dictionary.getSerdeLookup().apply(attributeTypeClass);
        if (serde != null) {
            attributeTypeClass = getSerdeSerializedClass(serde);
        }

        Schema<?> attribute = super.resolve(new AnnotatedType().type(attributeTypeClass), context, next);
        if (attribute == null) {
            attribute = super.resolve(new AnnotatedType().resolveAsRef(true).type(attributeTypeClass), context, next);
        }
        String description = getFieldDescription(clazzType, attributeName);
        String permissions = getFieldPermissions(clazzType, attributeName);

        attribute.setDescription(StringUtils.defaultIfEmpty(joinNonEmpty("\n", description, permissions), null));
        attribute.setExample(StringUtils.defaultIfEmpty(getFieldExample(clazzType, attributeName), null));
        attribute.setReadOnly(getFieldReadOnly(clazzType, attributeName));
        attribute.setWriteOnly(getFieldWriteOnly(clazzType, attributeName));
        attribute.setRequired(getFieldRequiredProperties(clazzType, attributeName));

        if (getFieldRequired(clazzType, attributeName)) {
            required.add(attributeName);
        }
        return attribute;
    }

    private Relationship processRelationship(Type<?> clazz, String relationshipName, Type<?> relationshipClazz,
            List<String> required) {
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
        relationship.setWriteOnly(getFieldWriteOnly(clazz, relationshipName));
        relationship.setRequired(getFieldRequiredProperties(clazz, relationshipName));

        if (getFieldRequired(clazz, relationshipName)) {
            required.add(relationshipName);
        }
        return relationship;
    }

    private Include getInclude(Type<?> clazz) {
        return dictionary.getAnnotation(clazz, Include.class);
    }

    private io.swagger.v3.oas.annotations.media.Schema getSchema(Type<?> clazz) {
        return dictionary.getAnnotation(clazz, io.swagger.v3.oas.annotations.media.Schema.class);
    }

    private String getSchemaDescription(Type<?> clazz) {
        io.swagger.v3.oas.annotations.media.Schema schema = getSchema(clazz);
        if (schema == null) {

            String description = EntityDictionary.getEntityDescription(clazz);

            if (StringUtils.isEmpty(description)) {
                return null;
            }

            return description;
        }
        return schema.description();
    }

    private io.swagger.v3.oas.annotations.media.Schema getSchema(Type<?> clazz, String fieldName) {
        return dictionary.getAttributeOrRelationAnnotation(clazz, io.swagger.v3.oas.annotations.media.Schema.class,
                fieldName);
    }

    private List<String> getFieldRequiredProperties(Type<?> clazz, String fieldName) {
        io.swagger.v3.oas.annotations.media.Schema property = getSchema(clazz, fieldName);
        return property != null ? Arrays.asList(property.requiredProperties()) : Collections.emptyList();
    }

    @SuppressWarnings("deprecation")
    private boolean getFieldRequired(Type<?> clazz, String fieldName) {
        io.swagger.v3.oas.annotations.media.Schema property = getSchema(clazz, fieldName);
        return property != null && (RequiredMode.REQUIRED.equals(property.requiredMode()) || property.required());
    }

    @SuppressWarnings("deprecation")
    private boolean getFieldReadOnly(Type<?> clazz, String fieldName) {
        io.swagger.v3.oas.annotations.media.Schema property = getSchema(clazz, fieldName);
        return property != null && (AccessMode.READ_ONLY.equals(property.accessMode()) || property.readOnly());
    }

    @SuppressWarnings("deprecation")
    private boolean getFieldWriteOnly(Type<?> clazz, String fieldName) {
        io.swagger.v3.oas.annotations.media.Schema property = getSchema(clazz, fieldName);
        return property != null && (AccessMode.WRITE_ONLY.equals(property.accessMode()) || property.writeOnly());
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
