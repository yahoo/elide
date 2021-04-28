/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import com.yahoo.elide.annotation.ApiVersion;
import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.annotation.NamespaceMeta;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Set;
import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 * Namespace for organizing related tables together.
 */
@Include(type = "namespace")
@Getter
@EqualsAndHashCode
@ToString
public class Namespace {
    @Id
    private final String id;

    private final String name;

    private final String friendlyName;

    private final String description;

    @Exclude
    private final String version;

    @OneToMany
    private Set<Table> tables;

    public Namespace(com.yahoo.elide.core.type.Package pkg) {
        id = pkg.getName();
        name = pkg.getName();

        NamespaceMeta meta = pkg.getDeclaredAnnotation(NamespaceMeta.class);
        if (meta != null) {
            friendlyName = meta.friendlyName();
            description = meta.description();
        } else {
            friendlyName = pkg.getName();
            description = null;
        }

        ApiVersion apiVersion = pkg.getDeclaredAnnotation(ApiVersion.class);
        version = (apiVersion == null) ? EntityDictionary.NO_VERSION : apiVersion.version();
    }

    public void addTable(Table table) {
        this.tables.add(table);
    }
}
