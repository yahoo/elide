/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@ReadPermission(expression = "deny all")
@Include(rootLevel = true, type = "noread") // optional here because class has this name
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
