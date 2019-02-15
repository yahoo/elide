/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * No Read test bean.
 */
@ReadPermission(expression = "deny all")
@Include(rootLevel = true, type = "noread") // optional here because class has this name
// Hibernate
@Entity
@Table(name = "noread")
public class NoReadEntity extends BaseId {
    @Column
    protected String field;

    @OneToOne(fetch = FetchType.LAZY)
    protected Child child;
}
