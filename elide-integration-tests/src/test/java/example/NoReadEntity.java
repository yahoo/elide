/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.security.checks.prefab.Role;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * No Read test bean.
 */
@ReadPermission(all = { Role.NONE.class })
@Include(rootLevel = true, type = "noread") // optional here because class has this name
// Hibernate
@Entity
@Table(name = "noread")
public class NoReadEntity {
    private long id;
    public String field;

    @OneToOne(fetch = FetchType.LAZY)
    public Child child;

    @Id @GeneratedValue
    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
}
