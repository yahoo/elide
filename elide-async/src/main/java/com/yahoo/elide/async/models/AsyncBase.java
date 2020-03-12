package com.yahoo.elide.async.models;

import java.util.Date;
import java.util.UUID;

import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

import com.yahoo.elide.annotation.Exclude;

@MappedSuperclass
public abstract class AsyncBase {

    private Date createdOn;

    private Date updatedOn;

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
        if (obj == null || !(obj instanceof AsyncQuery)) {
            return false;
        }

        return ((AsyncBase) obj).naturalKey.equals(naturalKey);
    }
}
