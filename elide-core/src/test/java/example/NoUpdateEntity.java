/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.UpdatePermission;

import lombok.Data;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@UpdatePermission(expression = "deny all")
@Include(rootLevel = true, type = "noupdate") // optional here because class has this name
// Hibernate
@Entity
@Table(name = "noupdate")
@Data
public class NoUpdateEntity {
    @Id
    private long id;

    @OneToMany()
    private Set<Child> children;
}
