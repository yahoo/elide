/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.security.permissions;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.NonTransferable;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.PersistentResource;
import com.yahoo.elide.core.type.Type;
import com.google.common.collect.ImmutableMap;

import lombok.Getter;

import java.lang.annotation.Annotation;
import java.util.Optional;

/**
 * Describes the state when a permission is evaluated.
 */
public class PermissionCondition {
    @Getter final Class<? extends Annotation> permission;
    @Getter final Type<?> entityClass;
    @Getter final Optional<PersistentResource> resource;
    @Getter final Optional<ChangeSpec> changes;
    @Getter final Optional<String> field;

    private static final ImmutableMap<Class<? extends Annotation>, String> PERMISSION_TO_NAME = ImmutableMap.of(
            ReadPermission.class, "READ",
            UpdatePermission.class, "UPDATE",
            DeletePermission.class, "DELETE",
            CreatePermission.class, "CREATE",
            NonTransferable.class, "NO TRANSFER");

    /**
     * This function attempts to create the appropriate {@link PermissionCondition} based on parameters that may or may
     * not be null. This is a temporary workaround given that the caller functions duplicate data in their
     * signatures and pass nulls.  The calling code needs to be cleaned up - and then this function can be disposed of.
     *
     * @param permission the permission to inspect
     * @param resource the resource to evalute the permission on
     * @param field the name of the field to be checked
     * @param changes the changes that happened
     * @return a {@link PermissionCondition} if one can be created, null otherwise
     */
    public static PermissionCondition create(
            Class<? extends Annotation> permission,
            PersistentResource resource,
            String field,
            ChangeSpec changes
    ) {
        if (resource != null) {
            if (changes != null) {
                /* Tests do this - not sure if this is real */
                if (field != null && changes.getFieldName() == null) {
                    return new PermissionCondition(permission, resource, field);
                }
                return new PermissionCondition(permission, resource, changes);
            }
            if (field == null) {
                return new PermissionCondition(permission, resource);
            }
            return new PermissionCondition(permission, resource, field);
        }
        throw new IllegalArgumentException("Resource cannot be null");
    }

    PermissionCondition(Class<? extends Annotation> permission, PersistentResource resource) {
        this.permission = permission;
        this.resource = Optional.of(resource);
        this.entityClass = resource.getResourceType();
        this.changes = Optional.empty();
        this.field = Optional.empty();
    }

    PermissionCondition(Class<? extends Annotation> permission, PersistentResource resource, ChangeSpec changes) {
        this.permission = permission;
        this.resource = Optional.of(resource);
        this.entityClass = resource.getResourceType();
        this.changes = Optional.of(changes);
        this.field = Optional.ofNullable(changes.getFieldName());
    }

    PermissionCondition(Class<? extends Annotation> permission, Type<?> entityClass) {
        this.permission = permission;
        this.resource = Optional.empty();
        this.entityClass = entityClass;
        this.changes = Optional.empty();
        this.field = Optional.empty();
    }

    PermissionCondition(Class<? extends Annotation> permission, Type<?> entityClass, String field) {
        this.permission = permission;
        this.resource = Optional.empty();
        this.entityClass = entityClass;
        this.changes = Optional.empty();
        this.field = Optional.of(field);
    }

    PermissionCondition(Class<? extends Annotation> permission, PersistentResource resource, String field) {
        this.permission = permission;
        this.resource = Optional.of(resource);
        this.entityClass = resource.getResourceType();
        this.changes = Optional.empty();
        this.field = Optional.of(field);
    }

    @Override
    public String toString() {
        Object entity = ((Optional) resource).orElse(entityClass);

        String withClause = changes.map(c -> String.format("WITH CHANGES %s", c))
                .orElseGet(() -> field.map(f -> String.format("WITH FIELD %s", f))
                .orElse(""));

        return String.format(
                "%s PERMISSION WAS INVOKED ON %s %s",
                PERMISSION_TO_NAME.get(permission),
                entity,
                withClause);
    }
}
