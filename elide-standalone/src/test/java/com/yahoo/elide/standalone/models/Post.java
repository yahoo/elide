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
import lombok.ToString;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Include(rootLevel = true)
@ToString
public class Post {
    private long id;
    private String content;
    private Date date;
    private boolean abusiveContent;

    @Id
    public long getId() {
            return id;
        }

    public void setId(long id) {
            this.id = id;
        }

    @Column(nullable = false)
    public String getContent() {
            return content;
        }

    @Temporal( TemporalType.TIMESTAMP )
    public Date getDate() {
            return date;
        }

    public void setDate(Date date) {
            this.date = date;
        }

    public void setContent(String content) {
            this.content = content;
    }

    @CreatePermission(expression = AdminCheck.USER_IS_ADMIN)
    @UpdatePermission(expression = AdminCheck.USER_IS_ADMIN)
    public boolean getAbusiveContent() {
        return abusiveContent;
    }

    public void setAbusiveContent(boolean abusiveContent) {
        this.abusiveContent = abusiveContent;
    }
}
