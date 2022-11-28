/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Exclude;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import java.util.UUID;

@MappedSuperclass
public abstract class BaseId {
    protected long id;
    protected String naturalKey = UUID.randomUUID().toString();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

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

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + ": " + id;
    }
}
