/*
 * Copyright 2021, Yahoo Inc.
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
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Model for Table Export.
 * TableExportHook is binded manually during the elide startup,
 * after asyncexecutorservice is initialized.
 */
@Entity
@Include(name = "tableExport", description = "Table export.", friendlyName = "TableExport")
@ReadPermission(expression = "Principal is Owner OR Principal is Admin")
@UpdatePermission(expression = "Prefab.Role.None")
@DeletePermission(expression = "Prefab.Role.None")
@Data
@EqualsAndHashCode(callSuper = true)
public class TableExport extends AsyncApi {

    @NotNull
    private String resultType; //CSV, JSON

    @Embedded
    private TableExportResult result;

    @Override
    public void setResult(AsyncApiResult result) {
        this.result = (TableExportResult) result;
    }
}
