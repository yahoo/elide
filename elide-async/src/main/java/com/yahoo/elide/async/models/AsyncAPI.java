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
import com.yahoo.elide.async.service.AsyncAPIUpdateThread;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.security.User;

import org.apache.http.NoHttpResponseException;

import lombok.Data;

import java.net.URISyntaxException;
import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.PreUpdate;
import javax.persistence.Transient;
import javax.validation.constraints.Max;
import javax.validation.constraints.Pattern;

/**
 * Base Model Class for Async Query.
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn
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

    private Date createdOn = new Date();

    private Date updatedOn = new Date();

    @Transient
    @Max(10)
    @ComputedAttribute
    private Integer asyncAfterSeconds = 10;

    @Exclude
    protected String naturalKey = UUID.randomUUID().toString();

    @Transient
    private AsyncAPIUpdateThread queryUpdateWorker = null;

    @Override
    public String getPrincipalName() {
        return principalName;
    }

    /**
     * Set Async API Result.
     * @param result Base Result Object to persist.
     */
    public abstract void setResult(AsyncAPIResult result);

    /**
     * Execute Async Request.
     * @param service AsyncExecutorService Instance.
     * @param user Elide User.
     * @param apiVersion Api Version.
     * @return Base Result Object.
     * @throws URISyntaxException URISyntaxException
     * @throws NoHttpResponseException NoHttpResponseException
     */
    public abstract AsyncAPIResult executeRequest(AsyncExecutorService service, User user, String apiVersion)
            throws URISyntaxException, NoHttpResponseException;

    @PreUpdate
    public void preUpdate() {
        this.updatedOn = new Date();
    }

    @Override
    public int hashCode() {
        return naturalKey.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof AsyncAPI) || this.getClass() != obj.getClass()) {
            return false;
        }

        return ((AsyncAPI) obj).naturalKey.equals(naturalKey);
    }
}
