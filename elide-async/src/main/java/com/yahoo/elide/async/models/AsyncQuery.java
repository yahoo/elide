/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.models;

import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.OnCreatePostCommit;
import com.yahoo.elide.annotation.OnCreatePreSecurity;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.core.RequestScope;

import lombok.Data;

import java.util.UUID;

import javax.inject.Inject;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.Transient;

/**
 * Model for Async Query.
 */
@Entity
@Include(type = "asyncQuery", rootLevel = true)
@ReadPermission(expression = "Principal is Owner")
@UpdatePermission(expression = "Prefab.Role.None")
@DeletePermission(expression = "Prefab.Role.None")
@Data
public class AsyncQuery extends AsyncBase implements PrincipalOwned {
    @Id
    @Column(columnDefinition = "varchar(36)")
    private UUID id; //Can be generated or provided.

    private String query;  //JSON-API PATH or GraphQL payload.

    private QueryType queryType; //GRAPHQL, JSONAPI

    @UpdatePermission(expression = "Principal is Owner AND value is Cancelled")
    private QueryStatus status;

    @OneToOne(mappedBy = "query", cascade = CascadeType.REMOVE)
    private AsyncQueryResult result;

    @Inject
    @Transient
    private AsyncExecutorService asyncExecutorService;

    @Exclude
    private String principalName;

    @PrePersist
    public void prePersistStatus() {
        status = QueryStatus.QUEUED;
    }

    @Override
    public String getPrincipalName() {
        return principalName;
    }

    @OnCreatePreSecurity
    public void extractPrincipalName(RequestScope scope) {
        setPrincipalName(scope.getUser().getName());
    }

    @OnCreatePostCommit
    public void executeQueryFromExecutor(RequestScope scope) {
        asyncExecutorService.executeQuery(this, scope.getUser());
    }
}
