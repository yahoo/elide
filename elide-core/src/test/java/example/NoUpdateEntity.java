/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.UpdatePermission;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.Set;

@UpdatePermission(expression = "Prefab.Role.None")
@Include(name = "noupdate") // optional here because class has this name
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
