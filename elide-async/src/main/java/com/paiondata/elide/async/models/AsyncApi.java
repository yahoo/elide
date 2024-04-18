/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.models;

import com.paiondata.elide.annotation.ComputedAttribute;
import com.paiondata.elide.annotation.CreatePermission;
import com.paiondata.elide.annotation.Exclude;
import com.paiondata.elide.annotation.UpdatePermission;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.Date;
import java.util.UUID;

/**
 * Base Model Class for Async Query.
 */
@MappedSuperclass
@Data
public abstract class AsyncApi implements PrincipalOwned {
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
    public abstract void setResult(AsyncApiResult result);

    /**
     * Get Async API Result.
     * @return AsyncApiResult object.
     */
    public abstract AsyncApiResult getResult();

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
        return obj instanceof AsyncApi && this.getClass() == obj.getClass() && id.equals(((AsyncApi) obj).id);
    }
}
