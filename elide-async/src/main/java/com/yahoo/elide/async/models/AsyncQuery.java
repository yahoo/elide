/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yahoo.elide.annotation.ComputedAttribute;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;

import lombok.Data;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.validation.constraints.Pattern;
import javax.persistence.Transient;
import javax.validation.constraints.Max;

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
    private UUID id; //Provided by client or generated if missing on create.

    private String query;  //JSON-API PATH or GraphQL payload.

    private QueryType queryType; //GRAPHQL, JSONAPI

    @Transient
    @Max(10)
    @ComputedAttribute
    private Integer asyncAfterSeconds = 10;

    private String requestId; //Client provided
    
    @UpdatePermission(expression = "Principal is Owner AND value is Cancelled")
    private QueryStatus status;

    @OneToOne(mappedBy = "query", cascade = CascadeType.REMOVE)
    private AsyncQueryResult result;

    @Exclude
    private String principalName;

    @PrePersist
    public void prePersistStatus() {
        status = QueryStatus.QUEUED;
        System.out.println(id);
        if(id == null) {
        	id = UUID.randomUUID();
        }
        System.out.println(id);
    }

    @Override
    public String getPrincipalName() {
        return principalName;
    }
}
