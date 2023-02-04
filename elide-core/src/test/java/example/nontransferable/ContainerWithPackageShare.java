/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.nontransferable;

import com.yahoo.elide.annotation.NonTransferable;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import java.util.Collection;

/**
 * Container for ShareableWithPackageShare and Untransferable.
 */
@Entity
@NonTransferable(enabled = false)
public class ContainerWithPackageShare {
    private long id;
    private Collection<Untransferable> untransferables;
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
    public Collection<Untransferable> getUntransferables() {
        return untransferables;
    }

    public void setUntransferables(Collection<Untransferable> unshareables) {
        this.untransferables = unshareables;
    }

    @OneToMany(fetch = FetchType.LAZY)
    public Collection<ShareableWithPackageShare> getShareableWithPackageShares() {
        return shareableWithPackageShares;
    }

    public void setShareableWithPackageShares(Collection<ShareableWithPackageShare> shareableWithPackageShare) {
        this.shareableWithPackageShares = shareableWithPackageShare;
    }
}
