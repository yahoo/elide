/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.models;

import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Model for Async Query.
 * AsyncQueryHook is binded manually during the elide startup,
 * after asyncexecutorservice is initialized.
 */
@Entity
@Include(name = "asyncQuery", description = "Async query.", friendlyName = "AsyncQuery")
@ReadPermission(expression = "Principal is Owner OR Principal is Admin")
@UpdatePermission(expression = "Prefab.Role.None")
@DeletePermission(expression = "Prefab.Role.None")
@Data
@EqualsAndHashCode(callSuper = true)
public class AsyncQuery extends AsyncApi {
    @Embedded
    private AsyncQueryResult result;

    @Override
    public void setResult(AsyncApiResult result) {
        this.result = (AsyncQueryResult) result;
    }
}
