/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import lombok.ToString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JSON API Document.
 */
@ToString
public class JsonApiDocument {
    private Data<Resource> data;
    private Meta meta;
    private final Map<String, String> links;
    private final LinkedHashSet<Resource> includedRecs;
    private final List<Resource> included;

    public JsonApiDocument() {
        this(null);
    }

    public JsonApiDocument(Data<Resource> data) {
        links = new LinkedHashMap<>();
        included = new ArrayList<>();
        includedRecs = new LinkedHashSet<>();
        this.data = data;
    }

    public void setData(Data<Resource> data) {
        this.data = data;
    }

    public Data<Resource> getData() {
        return data;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    @JsonInclude(Include.NON_NULL)
    public Meta getMeta() {
        return meta;
    }

    @JsonInclude(Include.NON_NULL)
    public Map<String, String> getLinks() {
        return links.isEmpty() ? null : links;
    }

    public void addLink(String key, String val) {
        this.links.put(key, val);
    }

    @JsonInclude(Include.NON_NULL)
    public List<Resource> getIncluded() {
        return included.isEmpty() ? null : included;
    }

    public void addIncluded(Resource resource) {
        if (!includedRecs.contains(resource)) {
            included.add(resource);
            includedRecs.add(resource);
        }
    }

    @Override
    public int hashCode() {
        Collection<Resource> resources = data == null ? null : data.get();
        return new HashCodeBuilder(37, 79)
            .append(resources == null ? 0 : resources.stream().mapToInt(Object::hashCode).sum())
            .append(links)
            .append(included == null ? 0 : included.stream().mapToInt(Object::hashCode).sum())
            .build();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof JsonApiDocument)) {
            return false;
        }
        JsonApiDocument other = (JsonApiDocument) obj;
        Collection<Resource> resources = Optional.ofNullable(data).map(Data::get).orElseGet(Collections::emptySet);
        Collection<Resource> otherResources =
                Optional.ofNullable(other.data).map(Data::get).orElseGet(Collections::emptySet);

        if (resources.size() != otherResources.size() || !resources.stream().allMatch(otherResources::contains)) {
            return false;
        }
        // TODO: Verify links and meta?
        if (other.getIncluded() == null) {
            return included.isEmpty();
        }
        return included.stream().allMatch(other.getIncluded()::contains);
    }
}
