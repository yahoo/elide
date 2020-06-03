/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.models;

import com.yahoo.elide.annotation.ComputedAttribute;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;

import lombok.Data;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Transient;
import javax.validation.constraints.Max;
import javax.validation.constraints.Pattern;

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
    @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$",
    message = "id not of pattern UUID")
    private String id; //Provided by client or generated if missing on create.

    private String query;  //JSON-API PATH or GraphQL payload.

    private QueryType queryType; //GRAPHQL, JSONAPI

    private ResultFormatType resultFormatType;

    @Transient
    @Max(10)
    @ComputedAttribute
    private Integer asyncAfterSeconds = 10;

    private String requestId; //Client provided

    @UpdatePermission(expression = "Principal is Owner AND value is Cancelled")
    private QueryStatus status;

    @Embedded
    private AsyncQueryResult result;

    @Exclude
    private String principalName;

    @PrePersist
    public void prePersistStatus() {
        status = QueryStatus.QUEUED;
        if (id == null || id.isEmpty() || id.equalsIgnoreCase("string")) {
            id = UUID.randomUUID().toString();
        }
    }

    @Override
    public String getPrincipalName() {
        return principalName;
    }
}
