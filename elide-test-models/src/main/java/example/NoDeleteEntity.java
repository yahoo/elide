/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Include;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@DeletePermission(expression = "deny all")
@Include(rootLevel = true, type = "nodelete") // optional here because class has this name
// Hibernate
@Entity
@Table(name = "nodelete")
public class NoDeleteEntity {
    private long id;

    @Id
    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
}
