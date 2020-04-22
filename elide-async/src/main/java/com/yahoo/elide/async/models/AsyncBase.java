/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.models;

import com.yahoo.elide.annotation.Exclude;

import lombok.Getter;

import java.util.Date;
import java.util.UUID;

import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

@MappedSuperclass
public abstract class AsyncBase {

    @Getter private Date createdOn;

    @Getter private Date updatedOn;

    @Exclude
    protected String naturalKey = UUID.randomUUID().toString();

    @PrePersist
    public void prePersist() {
        createdOn = updatedOn = new Date();
    }

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
