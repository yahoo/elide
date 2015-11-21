/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yahoo.elide.annotation.Include;

import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;


/**
 * Embedded test bean.
 */
@Include(rootLevel = true)
@Entity
public class Embedded {
    @JsonIgnore
    private long id;
    private Set<Long> segmentIds;

    public void setId(long id) {
        this.id = id;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long getId() {
        return id;
    }

    @ElementCollection
    public Set<Long> getSegmentIds() {
        return segmentIds;
    }

    public void setSegmentIds(Set<Long> segmentIds) {
        this.segmentIds = segmentIds;
    }

    public void addSegmentId(long segmentId) {
        segmentIds.add(segmentId);
    }
}
