/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import lombok.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents the path in an object relationship graph starting at an object
 * accessible from the root of the URL path and ending (exclusive) at this object.
 * These lineage paths are used for both security checks and audit logging.
 */
public class ResourceLineage {
    private final LinkedMap<String, List<PersistentResource>> resourceMap;
    private final List<LineagePath> resourcePath;

    /**
     * A node in the lineage that includes the resource and the link to the next node.
     */
    @Value
    public static class LineagePath {
        private PersistentResource resource;

        //The relationship that links this path element to the next.
        private String relationship;
    }

    /**
     * Empty lineage for objects rooted in the URL.
     */
    public ResourceLineage() {
        resourceMap = new LinkedMap<>();
        resourcePath = new LinkedList<>();
    }

    /**
     * Extends a lineage with a new resource.
     * @param sharedLineage the shared lineage
     * @param next the next
     * @param relationship The relationship name that links the lineage with the next element.
     */
    public ResourceLineage(ResourceLineage sharedLineage, PersistentResource next, String relationship) {
        resourceMap = new LinkedMap<>(sharedLineage.resourceMap);
        resourcePath = new LinkedList<>(sharedLineage.resourcePath);
        addRecord(next, relationship);
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
     *  Returns the immediate parent resource if one exists.
     * @return the parent or null if there is no parent.
     */
    public PersistentResource getParent() {
        if (resourcePath.isEmpty()) {
            return null;
        }

        return resourcePath.get(resourcePath.size() - 1).resource;
    }

    /**
     * Gets keys.
     *
     * @return the keys
     */
    public List<String> getKeys() {
        return resourceMap.asList();
    }

    public List<LineagePath> getResourcePath() {
        return resourcePath;
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
        return other.resourcePath.equals(this.resourcePath);
    }

    private void addRecord(PersistentResource latest, String relationship) {
        String alias = latest.getTypeName();
        List<PersistentResource> resources;
        if (resourceMap.containsKey(alias)) {
            resources = resourceMap.get(alias);
        } else {
            resources = new ArrayList<>();
            resourceMap.put(alias, resources);
        }
        resources.add(latest);
        resourcePath.add(new ResourceLineage.LineagePath(latest, relationship));
    }
}
