/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.models;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;

import lombok.Data;

/**
 * Model for Async Query Result
 */
@Entity
@Include(type="queryResult")
@ReadPermission(expression = "Principal is Owner")
@UpdatePermission(expression = "Prefab.Role.None")
@CreatePermission(expression = "Prefab.Role.None")
@DeletePermission(expression = "Prefab.Role.None")
@Data
public class AsyncQueryResult implements PrincipalOwned {
    @Id
    private UUID id; //Matches UUID in query.

    private Integer contentLength;

    private String responseBody; //success or errors

    private Integer status; // HTTP Status

    private Date createdOn;

    private Date updatedOn;

    @OneToOne
    private AsyncQuery query;

    @Exclude
    protected String naturalKey = UUID.randomUUID().toString();

    @Exclude
    public String getPrincipalName() {
        return query.getPrincipalName();
    }

    @PrePersist
    public void prePersist() {
        createdOn = updatedOn = new Date();
    }

    @PreUpdate
    public void preUpdate() {
        updatedOn = new Date();
    }

    @Override
    public int hashCode() {
        return naturalKey.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof AsyncQuery)) {
            return false;
        }

        return ((AsyncQuery) obj).naturalKey.equals(naturalKey);
    }
}