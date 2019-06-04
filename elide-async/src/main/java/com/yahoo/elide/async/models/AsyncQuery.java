/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.models;

import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;

import lombok.Data;

import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;

/**
 * Model for Async Query.
 * ExecuteQueryHook and UpdatePrincipalNameHook is binded manually during the elide startup,
 * after asyncexecutorservice is initialized.
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
    private UUID id; //Provided.

    private String query;  //JSON-API PATH or GraphQL payload.

    private QueryType queryType; //GRAPHQL, JSONAPI

    @UpdatePermission(expression = "Principal is Owner AND value is Cancelled")
    private QueryStatus status;

    @OneToOne(mappedBy = "query", cascade = CascadeType.REMOVE)
    private AsyncQueryResult result;

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
}
