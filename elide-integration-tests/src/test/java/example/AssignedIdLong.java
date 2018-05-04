/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Test Bean without GeneratedValue Id.
 */
@Entity
@Table(name = "assigned_id_long")
@Include(rootLevel = true)
public class AssignedIdLong {
    private long id;
    private int value;

    @Id
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof AssignedIdLong)) {
            return false;
        }

        AssignedIdLong other = (AssignedIdLong) obj;

        return id == other.id;
    }
}
