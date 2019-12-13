/*
 * Copyright 2018, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.standalone.models;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.standalone.checks.AdminCheck;
import lombok.Data;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Include(rootLevel = true)
@Data
public class Post {
    @Id
    private long id;

    @Column(nullable = false)
    private String content;

    @Temporal( TemporalType.TIMESTAMP )
    private Date date;

    @CreatePermission(expression = AdminCheck.USER_IS_ADMIN)
    @UpdatePermission(expression = AdminCheck.USER_IS_ADMIN)
    private boolean abusiveContent;
}
