/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the path in an object relationship graph starting at an object
 * accessible from the root of the URL path and ending (exclusive) at this object.
 * These lineage paths are used for both security checks and audit logging.
 */
public class ResourceLineage {
    private final LinkedMap<String, List<PersistentResource>> resourceMap;

    /**
     * Empty lineage for objects rooted in the URL.
     */
    public ResourceLineage() {
        resourceMap = new LinkedMap<>();
    }

    /**
     * Extends a lineage with a new resource.
     * @param sharedLineage the shared lineage
     * @param next the next
     */
    public ResourceLineage(ResourceLineage sharedLineage, PersistentResource next) {
        resourceMap = new LinkedMap<>(sharedLineage.resourceMap);
        addRecord(next);
    }

    /**
     * Extends a lineage with a new resource that has an alias.  An alias is useful if
     * there are two objects in the lineage with the same type or class.  The resource
     * lineage does not allow two resources to have the same name.  As long as one of the two
     * resources can be given an alias (by annotation), then two objects with the same type/class
     * can exist in the lineage.  This case is likely to be uncommon.
     * @param sharedLineage the shared lineage
     * @param next the next
     * @param nextAlias the next alias
     */
    public ResourceLineage(ResourceLineage sharedLineage, PersistentResource next, String nextAlias) {
        resourceMap = new LinkedMap<>(sharedLineage.resourceMap);
        addRecord(next, nextAlias);
    }

    /**
     * Gets record.
     *
     * @param name the name
     * @return the record
     */
    public List<PersistentResource> getRecord(String name) {
        List<PersistentResource> list = resourceMap.get(name);
        return list == null ? Collections.emptyList() : list;
    }

    /**
     * Gets keys.
     *
     * @return the keys
     */
    public List<String> getKeys() {
        return resourceMap.asList();
    }

    private void addRecord(PersistentResource latest) {
        addRecord(latest, latest.getType());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 123)
            .append(getKeys())
            .append(resourceMap.asList())
            .toHashCode();
    }

    @Override
    public boolean equals(Object input) {
        if (!(input instanceof ResourceLineage)) {
            return false;
        }
        ResourceLineage other = (ResourceLineage) input;
        if (other.getKeys().size() != this.getKeys().size()) {
            return false;
        }
        for (String key : other.getKeys()) {
            if (!other.getRecord(key).equals(this.getRecord(key))) {
                return false;
            }
        }
        return true;
    }

    private void addRecord(PersistentResource latest, String alias) {
        List<PersistentResource> resources;
        if (resourceMap.containsKey(alias)) {
            resources = resourceMap.get(alias);
        } else {
            resources = new ArrayList<>();
            resourceMap.put(alias, resources);
        }
        resources.add(latest);
    }
}
