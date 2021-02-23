/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.nontransferable;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.NonTransferable;

import java.util.Collection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 * Container for ShareableWithPackageShare and Untransferable.
 */
@Entity
@Include
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
