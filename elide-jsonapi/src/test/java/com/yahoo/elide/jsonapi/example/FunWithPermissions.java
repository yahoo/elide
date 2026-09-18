/*
 * Copyright 2025, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.core.security.checks.UserCheck;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

import java.util.Set;

/**
 * Minimal entity with one openly-readable and one permission-restricted attribute and relationship, used to verify
 * that {@link com.yahoo.elide.jsonapi.JsonApiPersistentResource} applies per-request permission filtering.
 */
@Entity
@Include(name = "funWithPermissions")
public class FunWithPermissions {

    private long id;
    private String openField;
    private String restrictedField;
    private Set<FunWithPermissions> openRelation;
    private Set<FunWithPermissions> restrictedRelation;
    private Set<FunWithPermissions> forbiddenRelation;

    @Id
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @ReadPermission(expression = "Prefab.Role.All")
    public String getOpenField() {
        return openField;
    }

    public void setOpenField(String openField) {
        this.openField = openField;
    }

    @ReadPermission(expression = "goodUser")
    public String getRestrictedField() {
        return restrictedField;
    }

    public void setRestrictedField(String restrictedField) {
        this.restrictedField = restrictedField;
    }

    @ReadPermission(expression = "Prefab.Role.All")
    @ManyToMany
    public Set<FunWithPermissions> getOpenRelation() {
        return openRelation;
    }

    public void setOpenRelation(Set<FunWithPermissions> openRelation) {
        this.openRelation = openRelation;
    }

    @ReadPermission(expression = "goodUser")
    @ManyToMany
    public Set<FunWithPermissions> getRestrictedRelation() {
        return restrictedRelation;
    }

    public void setRestrictedRelation(Set<FunWithPermissions> restrictedRelation) {
        this.restrictedRelation = restrictedRelation;
    }

    @ReadPermission(expression = "Prefab.Role.None")
    @ManyToMany
    public Set<FunWithPermissions> getForbiddenRelation() {
        return forbiddenRelation;
    }

    public void setForbiddenRelation(Set<FunWithPermissions> forbiddenRelation) {
        this.forbiddenRelation = forbiddenRelation;
    }

    public static class GoodUserCheck extends UserCheck {
        @Override
        public boolean ok(User user) {
            return "1".equals(user.getName());
        }
    }
}
