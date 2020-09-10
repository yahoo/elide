/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.models;

import com.yahoo.elide.annotation.ComputedAttribute;
import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;

import com.yahoo.elide.async.service.AsyncQueryUpdateThread;

import lombok.Data;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Transient;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * Model for Async Query.
 * ExecuteQueryHook and UpdatePrincipalNameHook is binded manually during the elide startup,
 * after asyncexecutorservice is initialized.
 */
@Entity
@Include(type = "asyncQuery", rootLevel = true)
@ReadPermission(expression = "Principal is Owner OR Principal is Admin")
@UpdatePermission(expression = "Prefab.Role.None")
@DeletePermission(expression = "Prefab.Role.None")
@Data
public class AsyncQuery extends AsyncBase implements PrincipalOwned {
    @Id
    @Column(columnDefinition = "varchar(36)")
    @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$",
    message = "id not of pattern UUID")
    private String id = UUID.randomUUID().toString(); //Provided by client or generated if missing on create.

    private String query;  //JSON-API PATH or GraphQL payload.

    private QueryType queryType; //GRAPHQL, JSONAPI

    private ResultFormatType resultFormatType = ResultFormatType.JSONAPI;

    @Transient
    @Max(10)
    @ComputedAttribute
    private Integer asyncAfterSeconds = 10;

    @Exclude
    @Column(columnDefinition = "varchar(36)")
    private String requestId = UUID.randomUUID().toString();

    @UpdatePermission(expression = "(Principal is Admin OR Principal is Owner) AND value is Cancelled")
    @CreatePermission(expression = "value is Queued")
    @Enumerated(EnumType.STRING)
    private QueryStatus status = QueryStatus.QUEUED;

    @Enumerated(EnumType.STRING)
    @NotNull
    private ResultType resultType; //EMBEDDED, DOWNLOAD

    @Embedded
    private AsyncQueryResult result;

    @CreatePermission(expression = "Prefab.Role.None")
    private String principalName;

    @Transient
    private AsyncQueryUpdateThread queryUpdateWorker = null;

    @Override
    public String getPrincipalName() {
        return principalName;
    }
}
