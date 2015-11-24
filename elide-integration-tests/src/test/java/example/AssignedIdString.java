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
@Table(name = "assigned_id_string")
@Include(rootLevel = true)
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
}
