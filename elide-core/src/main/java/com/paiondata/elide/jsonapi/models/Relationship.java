/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi.models;

import com.paiondata.elide.core.PersistentResource;
import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.exceptions.ForbiddenAccessException;
import com.paiondata.elide.core.exceptions.InvalidObjectIdentifierException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.collections4.MapUtils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Model representing JSON API Relationship.
 */
public class Relationship {
    private final Map<String, String> links;
    private final Data<Resource> data; // NOTE: Our serializer handles resources so that's what we store
    @JsonIgnore private final Data<ResourceIdentifier> idData;

    // NOTE: We take in a Resource instead of ResourceIdentifier here due to a deserialization conflict
    public Relationship(@JsonProperty("links") Map<String, String> links,
                        @JsonProperty("data") Data<Resource> data) {
        this.links = links;
        this.data = data;
        if (data != null) {
            if (data.isToOne()) {
                Resource resource = data.getSingleValue();
                this.idData = new Data<>(resource != null ? resource.toResourceIdentifier() : null);
            } else {
                this.idData = new Data<>(
                        data.get().stream()
                            .map(Objects::requireNonNull)
                            .map(Resource::toResourceIdentifier)
                            .collect(Collectors.toList())
                );
            }
        } else {
            this.idData = null;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<String, String> getLinks() {
        return MapUtils.isEmpty(links) ? null : links;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Data<Resource> getData() {
        return data;
    }

    @JsonIgnore
    public Data<ResourceIdentifier> getResourceIdentifierData() {
        return idData;
    }

    public Set<PersistentResource> toPersistentResources(RequestScope requestScope)
        throws ForbiddenAccessException, InvalidObjectIdentifierException {
        Set<PersistentResource> res = new LinkedHashSet<>();
        if (data == null) {
            return null;
        }
        Collection<Resource> resources = data.get();
        if (resources != null) {
            for (Resource resource : resources) {
                if (resource == null) {
                    continue;
                }
                res.add(resource.toPersistentResource(requestScope));
            }
        }
        return res.isEmpty() ? (data.isToOne() ? null : res) : res;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Data<Resource> that = ((Relationship) o).getData();

        if (that == null || data == null) {
            return that == data;
        }

        Collection<ResourceIdentifier> resourceIdentifiers = data.toResourceIdentifiers();
        Collection<ResourceIdentifier> theirIdentifiers = that.toResourceIdentifiers();

        return resourceIdentifiers.stream().allMatch(theirIdentifiers::contains);
    }

    @Override
    public int hashCode() {
        return (idData == null) ? -1 : idData.hashCode();
    }
}
