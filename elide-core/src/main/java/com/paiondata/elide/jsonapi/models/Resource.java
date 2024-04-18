/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi.models;

import com.paiondata.elide.core.PersistentResource;
import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.exceptions.ForbiddenAccessException;
import com.paiondata.elide.core.exceptions.InvalidObjectIdentifierException;
import com.paiondata.elide.core.exceptions.UnknownEntityException;
import com.paiondata.elide.core.request.EntityProjection;
import com.paiondata.elide.core.type.Type;
import com.paiondata.elide.jsonapi.serialization.KeySerializer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import lombok.ToString;

import java.util.Map;
import java.util.Objects;

/**
 * Resource wrapper around serialized/deserialized JSON API
 *
 * NOTE: We violate the DRY principle to create a clear separation of concern. That is,
 *       the Resource is a distinct from an internal Record. In fact, they are not
 *       interchangeable even though they represent very similar data
 */
@ToString
public class Resource {

    //Doesn't work currently - https://github.com/FasterXML/jackson-databind/issues/230
    @JsonProperty(required = true)
    private String type;
    private String id;
    private String lid;
    private Map<String, Object> attributes;
    private Map<String, Relationship> relationships;
    private Map<String, String> links;
    private Meta meta;

    public Resource(String type, String id) {
        this.type = type;
        this.id = id;
        if (id == null) {
            throw new InvalidObjectIdentifierException(id, type);
        }
    }

    public Resource(@JsonProperty("type") String type,
                    @JsonProperty("id") String id,
                    @JsonProperty("lid") String lid,
                    @JsonProperty("attributes") Map<String, Object> attributes,
                    @JsonProperty("relationships") Map<String, Relationship> relationships,
                    @JsonProperty("links") Map<String, String> links,
                    @JsonProperty("meta") Meta meta) {
        this.type = type;
        this.id = id;
        this.lid = lid;
        this.attributes = attributes;
        this.relationships = relationships;
        this.links = links;
        this.meta = meta;
        if (this.id == null) {
            this.id = lid;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getId() {
        return id;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getLid() {
        return lid;
    }

    public void setRelationships(Map<String, Relationship> relationships) {
        this.relationships = relationships;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<String, Relationship> getRelationships() {
        return MapUtils.isEmpty(relationships) ? null : relationships;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonSerialize(keyUsing = KeySerializer.class)
    public Map<String, Object> getAttributes() {
        return MapUtils.isEmpty(attributes) ? null : attributes;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setLid(String lid) {
        this.lid = lid;
    }

    public void setAttributes(Map<String, Object> obj) {
        this.attributes = obj;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<String, String> getLinks() {
        return links;
    }

    public void setLinks(Map<String, String> links) {
        this.links = links;
    }

    /**
     * Convert Resource to resource identifier.
     *
     * @return linkage
     */
    public ResourceIdentifier toResourceIdentifier() {
        return new ResourceIdentifier(type, id != null ? id : lid);
    }

    @Override
    public int hashCode() {
        // We hope that type and id are effectively final after jackson constructs the object...
        return new HashCodeBuilder(37, 17).append(type).append(id != null ? id : lid).build();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Resource) {
            Resource that = (Resource) obj;
            return Objects.equals(this.id, that.id)
                   && Objects.equals(this.lid, that.lid)
                   && Objects.equals(this.attributes, that.attributes)
                   && Objects.equals(this.type, that.type)
                   && Objects.equals(this.relationships, that.relationships);
        }
        return false;
    }

    public PersistentResource<?> toPersistentResource(RequestScope requestScope)
        throws ForbiddenAccessException, InvalidObjectIdentifierException {
        EntityDictionary dictionary = requestScope.getDictionary();

        Type<?> cls = dictionary.getEntityClass(type, requestScope.getRoute().getApiVersion());

        if (cls == null) {
            throw new UnknownEntityException(type);
        }
        String identifier = id != null ? id : lid;
        if (identifier == null) {
            throw new InvalidObjectIdentifierException(identifier, type);
        }

        EntityProjection projection = EntityProjection.builder()
            .type(cls)
            .build();

        return PersistentResource.loadRecord(projection, identifier, requestScope);
    }
}
