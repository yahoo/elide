/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.models;

import java.security.Principal;
import java.util.Date;
import java.util.UUID;

import javax.inject.Inject;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Transient;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.OnCreatePostCommit;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.core.RequestScope;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Model for Async Query
 */
@Entity
@Include(type = "query", rootLevel = true)
@ReadPermission(expression = "Principal is Owner")
@UpdatePermission(expression = "Prefab.Role.None")
@Slf4j
@Data
public class AsyncQuery implements PrincipalOwned {
    @Id
    private UUID id; //Can be generated or provided.

    //Extracted from the Principal object
    private String principalName;

    private String query;  //JSON-API PATH or GraphQL payload.

    private QueryType queryType; //GRAPHQL, JSONAPI

    @UpdatePermission(expression = "Principal is Owner AND value is Cancelled")
    private QueryStatus status;

    @OneToOne(mappedBy = "query")
    private AsyncQueryResult result;

    private Date createdOn;

    private Date updatedOn;

    @Inject
    @Transient
    private AsyncExecutorService asyncExecutorService;

    @Override
	public String getPrincipalName() {
		return principalName;
	}

    @PrePersist
    public void prePersist() {
        createdOn = updatedOn = new Date();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedOn = new Date();
    }

    @OnCreatePostCommit
    public void executeQueryFromExecutor(RequestScope scope) {
        log.info("AsyncExecutorService executor object: {}", asyncExecutorService);
        asyncExecutorService.executeQuery(query, queryType, (Principal) scope.getUser().getOpaqueUser(), id);
    }
}
