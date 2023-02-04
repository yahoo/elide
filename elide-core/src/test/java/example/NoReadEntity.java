/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@ReadPermission(expression = "Prefab.Role.None")
@Include(name = "noread") // optional here because class has this name
// Hibernate
@Entity
@Table(name = "noread")
@Data
public class NoReadEntity {
    @Id
    private long id;

    private String field;

    @OneToOne
    private Child child;
}
