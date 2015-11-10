/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.security.Role;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@CreatePermission(all = { Role.NONE.class })
@Include(rootLevel = true, type = "nocreate") // optional here because class has this name
// Hibernate
@Entity
@Table(name = "nocreate")
public class NoCreateEntity {
    private long id;

    @Id
    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
}
