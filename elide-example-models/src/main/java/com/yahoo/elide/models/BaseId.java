/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.models;

import com.yahoo.elide.annotation.Exclude;

import java.util.UUID;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * Base class for Entities that provides a hashcode and equals that will be the same
 * before and after an object is persisted to the DB.
 */
@MappedSuperclass
public abstract class BaseId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected long id;
    protected String naturalKey = UUID.randomUUID().toString();

    @Exclude
    public String getNaturalKey() {
        return naturalKey;
    }

    public void setNaturalKey(String naturalKey) {
        this.naturalKey = naturalKey;
    }

    @Override
    public int hashCode() {
        return naturalKey.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof BaseId)) {
            return false;
        }

        return ((BaseId) obj).naturalKey.equals(naturalKey);
    }
}
