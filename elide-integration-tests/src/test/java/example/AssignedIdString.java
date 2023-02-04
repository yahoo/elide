/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Test Bean without GeneratedValue Id.
 */
@Entity
@Table(name = "assigned_id_string")
@Include
public class AssignedIdString {
    private String id;
    private int value;

    @Id
    public String getId() {
        return id;
    }

    public void setId(String id) {
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
        if (id == null) {
            return super.hashCode();
        }
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (id == null) {
            return super.equals(obj);
        }

        return obj instanceof AssignedIdString && id.equals(((AssignedIdString) obj).id);
    }
}
