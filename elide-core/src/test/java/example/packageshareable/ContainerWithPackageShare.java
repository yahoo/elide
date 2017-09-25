/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.packageshareable;

import com.yahoo.elide.annotation.Include;

import java.util.Collection;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 * Container for ShareableWithPackageShare and UnshareableWithEntityUnshare.
 */
@Entity
@Include(rootLevel = true)
public class ContainerWithPackageShare {
    private long id;
    private Collection<UnshareableWithEntityUnshare> unshareableWithEntityUnshares;
    private Collection<ShareableWithPackageShare> shareableWithPackageShares;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @OneToMany(fetch = FetchType.LAZY)
    public Collection<UnshareableWithEntityUnshare> getUnshareableWithEntityUnshares() {
        return unshareableWithEntityUnshares;
    }

    public void setUnshareableWithEntityUnshares(Collection<UnshareableWithEntityUnshare> unshareables) {
        this.unshareableWithEntityUnshares = unshareables;
    }

    @OneToMany(fetch = FetchType.LAZY)
    public Collection<ShareableWithPackageShare> getShareableWithPackageShares() {
        return shareableWithPackageShares;
    }

    public void setShareableWithPackageShares(Collection<ShareableWithPackageShare> shareableWithPackageShare) {
        this.shareableWithPackageShares = shareableWithPackageShare;
    }
}
