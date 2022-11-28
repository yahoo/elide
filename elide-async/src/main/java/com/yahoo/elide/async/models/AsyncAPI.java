/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.models;

import com.yahoo.elide.annotation.ComputedAttribute;
import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.UpdatePermission;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Transient;
import lombok.Data;

import java.util.Date;
import java.util.UUID;

import javax.validation.constraints.Pattern;

/**
 * Base Model Class for Async Query.
 */
@MappedSuperclass
@Data
public abstract class AsyncAPI implements PrincipalOwned {
    @Id
    @Column(columnDefinition = "varchar(36)")
    @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$",
    message = "id not of pattern UUID")
    private String id = UUID.randomUUID().toString(); //Provided by client or generated if missing on create.

    protected String query;  //JSON-API PATH or GraphQL payload.

    protected QueryType queryType; //GRAPHQL, JSONAPI

    @Exclude
    @Column(columnDefinition = "varchar(36)")
    protected String requestId = UUID.randomUUID().toString();

    @CreatePermission(expression = "Prefab.Role.None")
    private String principalName;

    @UpdatePermission(expression = "(Principal is Admin OR Principal is Owner) AND value is Cancelled")
    @CreatePermission(expression = "value is Queued")
    @Enumerated(EnumType.STRING)
    private QueryStatus status = QueryStatus.QUEUED;

    @CreatePermission(expression = "Prefab.Role.None")
    @UpdatePermission(expression = "Prefab.Role.None")
    private Date createdOn = new Date();

    @CreatePermission(expression = "Prefab.Role.None")
    @UpdatePermission(expression = "Prefab.Role.None")
    private Date updatedOn = new Date();

    @Transient
    @ComputedAttribute
    private Integer asyncAfterSeconds = 10;

    /**
     * Set Async API Result.
     * @param result Base Result Object to persist.
     */
    public abstract void setResult(AsyncAPIResult result);

    /**
     * Get Async API Result.
     * @return AsyncAPIResult object.
     */
    public abstract AsyncAPIResult getResult();

    @PreUpdate
    public void preUpdate() {
        this.updatedOn = new Date();
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AsyncAPI && this.getClass() == obj.getClass() && id.equals(((AsyncAPI) obj).id);
    }
}
