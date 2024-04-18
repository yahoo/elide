/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.ToString;

/**
 * The reference that identifies the target of the operation.
 */
@ToString
@JsonPropertyOrder({"type", "id", "lid", "relationship"})
@JsonInclude(Include.NON_NULL)
public class Ref {

    private String type;
    private String id;
    private String lid;
    private String relationship;

    public Ref(@JsonProperty("type") String type,
            @JsonProperty("id") String id,
            @JsonProperty("lid") String lid,
            @JsonProperty("relationship") String relationship) {
        this.type = type;
        this.id = id;
        this.lid = lid;
        this.relationship = relationship;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLid() {
        return lid;
    }

    public void setLid(String lid) {
        this.lid = lid;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }
}
