/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.security.permissions;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.SharePermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.PersistentResource;
import lombok.Getter;

import java.lang.annotation.Annotation;
import java.util.Optional;

/**
 * Describes the state when a permission is evaluated.
 */
public class PermissionCondition {
    @Getter final Class<? extends Annotation> permission;
    @Getter final Class<?> entityClass;
    @Getter final Optional<PersistentResource> resource;
    @Getter final Optional<ChangeSpec> changes;
    @Getter final Optional<String> field;

    /**
     * This function attempts to create the appropriate PermissionCondition based on parameters that may or may
     * not be null. This is a temporary workaround given that the caller functions duplicate data in their
     * signatures and pass nulls.  The calling code needs to be cleaned up - and then this function can be disposed of.
     *
     * @param permission
     * @param resource
     * @param field
     * @param changes
     * @return
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
            } else if (field == null) {
                return new PermissionCondition(permission, resource);
            } else {
                return new PermissionCondition(permission, resource, field);
            }
        }
        throw new IllegalArgumentException("Resource cannot be null");
    }

    PermissionCondition(Class<? extends Annotation> permission, PersistentResource resource) {
        this.permission = permission;
        this.resource = Optional.of(resource);
        this.entityClass = resource.getResourceClass();
        this.changes = Optional.empty();
        this.field = Optional.empty();
    }

    PermissionCondition(Class<? extends Annotation> permission, PersistentResource resource, ChangeSpec changes) {
        this.permission = permission;
        this.resource = Optional.of(resource);
        this.entityClass = resource.getResourceClass();
        this.changes = Optional.of(changes);
        this.field = Optional.ofNullable(changes.getFieldName());
    }

    PermissionCondition(Class<? extends Annotation> permission, Class<?> entityClass) {
        this.permission = permission;
        this.resource = Optional.empty();
        this.entityClass = entityClass;
        this.changes = Optional.empty();
        this.field = Optional.empty();
    }

    PermissionCondition(Class<? extends Annotation> permission, PersistentResource resource, String field) {
        this.permission = permission;
        this.resource = Optional.of(resource);
        this.entityClass = resource.getResourceClass();
        this.changes = Optional.empty();
        this.field = Optional.of(field);
    }

    @Override
    public String toString() {
        Object entity = resource.isPresent() ? resource.get() : entityClass;

        String withChanges = changes.isPresent() ? String.format("WITH CHANGES %s", changes.get()) : "";
        String withField = field.isPresent() ? String.format("WITH FIELD %s", field.get()) : "";

        String withClause = withChanges.isEmpty() ? withField : withChanges;

        return String.format(
                "%s PERMISSION WAS INVOKED ON %s %s",
                permission2text(permission),
                entity,
                withClause);
    }

    private static String permission2text(Class<? extends Annotation> permission) {
        if (permission.equals(ReadPermission.class)) {
            return "READ";
        } else if (permission.equals(UpdatePermission.class)) {
            return "UPDATE";
        } else if (permission.equals(DeletePermission.class)) {
            return "DELETE";
        } else if (permission.equals(CreatePermission.class)) {
            return "CREATE";
        } else if (permission.equals(SharePermission.class)) {
            return "SHARE";
        } else {
            throw new IllegalArgumentException("Invalid annotation type");
        }

    }
}
