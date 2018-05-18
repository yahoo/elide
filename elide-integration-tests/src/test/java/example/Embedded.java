/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;

import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;

/**
 * Embedded test bean.
 */
@Include(rootLevel = true)
@Entity
public class Embedded extends BaseId {
    private Set<Long> segmentIds;

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
