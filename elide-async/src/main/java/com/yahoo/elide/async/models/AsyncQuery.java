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
import lombok.Data;

import javax.persistence.Embedded;
import javax.persistence.Entity;

/**
 * Model for Async Query.
 * AsyncQueryHook is binded manually during the elide startup,
 * after asyncexecutorservice is initialized.
 */
@Entity
@Include(type = "asyncQuery")
@ReadPermission(expression = "Principal is Owner OR Principal is Admin")
@UpdatePermission(expression = "Prefab.Role.None")
@DeletePermission(expression = "Prefab.Role.None")
@Data
public class AsyncQuery extends AsyncAPI {
    @Embedded
    private AsyncQueryResult result;

    @Override
    public void setResult(AsyncAPIResult result) {
        this.result = (AsyncQueryResult) result;
    }
}
