/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;

import java.util.Collection;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * Container for shareables and unshareables.
 */
@Entity
@Table(name = "container")
@Include(rootLevel = true, type = "container")
public class Container extends BaseId {
    private Collection<Unshareable> unshareables;
    private Collection<Shareable> shareables;

    @OneToMany(mappedBy = "container")
    public Collection<Unshareable> getUnshareables() {
        return unshareables;
    }

    public void setUnshareables(Collection<Unshareable> unshareables) {
        this.unshareables = unshareables;
    }

    @OneToMany(mappedBy = "container")
    public Collection<Shareable> getShareables() {
        return shareables;
    }

    public void setShareables(Collection<Shareable> shareables) {
        this.shareables = shareables;
    }
}
