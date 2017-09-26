/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.UpdatePermission;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.Set;

@UpdatePermission(expression = "deny all")
@Include(rootLevel = true, type = "noupdate") // optional here because class has this name
// Hibernate
@Entity
@Table(name = "noupdate")
public class NoUpdateEntity {
    private long id;

    @OneToMany()
    public Set<Child> children;

    @Id
    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
}
