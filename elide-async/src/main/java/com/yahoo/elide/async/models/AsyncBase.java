/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.models;

import com.yahoo.elide.annotation.Exclude;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

import javax.persistence.MappedSuperclass;
import javax.persistence.PreUpdate;

@MappedSuperclass
public abstract class AsyncBase {

    @Getter @Setter private Date createdOn = new Date();

    @Getter @Setter private Date updatedOn = new Date();

    @Exclude
    protected String naturalKey = UUID.randomUUID().toString();

    @PreUpdate
    public void preUpdate() {
        this.updatedOn = new Date();
    }

    @Override
    public int hashCode() {
        return naturalKey.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof AsyncBase) || this.getClass() != obj.getClass()) {
            return false;
        }

        return ((AsyncBase) obj).naturalKey.equals(naturalKey);
    }
}
