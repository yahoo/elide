/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.SharePermission;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@SharePermission(expression = "deny all")
@Include(rootLevel = true, type = "noshare") // optional here because class has this name
// Hibernate
@Entity
@Table(name = "noshare")
public class NoShareEntity {
    private long id;

    @Id
    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
}
