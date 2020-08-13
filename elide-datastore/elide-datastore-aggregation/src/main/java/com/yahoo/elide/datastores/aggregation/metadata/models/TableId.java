/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

import javax.persistence.Embeddable;

@SuppressWarnings("serial")
@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TableId implements Serializable {

    private String name;
    private String version;
    private String dbConnectionName;

    @Override
    public String toString() {
        return name + prefix(version, ".") + prefix(dbConnectionName, "-");
    }

    private String prefix(String str, String prefix) {
        return (str == null || str.trim().isEmpty()) ? "" : prefix + str;
    }
}
