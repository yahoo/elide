/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.UpdatePermission;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * No Update test bean.
 */
@UpdatePermission(expression = "deny all")
@Include(rootLevel = true, type = "noupdate") // optional here because class has this name
// Hibernate
@Entity
@Table(name = "noupdate")
public class NoUpdateEntity extends BaseId {
    @OneToMany
    protected Set<Child> children;
}
