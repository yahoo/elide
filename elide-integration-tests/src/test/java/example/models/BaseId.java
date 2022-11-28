/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.models;

import com.yahoo.elide.annotation.Exclude;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import java.util.UUID;

/**
 * Base class for Entities that provides a hashcode and equals that will be the same
 * before and after an object is persisted to the DB.
 */
@MappedSuperclass
public abstract class BaseId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected long id;

    @Exclude
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
        return obj instanceof BaseId && naturalKey.equals(((BaseId) obj).naturalKey);
    }
}
