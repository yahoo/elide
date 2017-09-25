/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.packageshareable;

import com.yahoo.elide.annotation.Include;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * Package level shareable bean.
 */
@Entity
@Include(rootLevel = true)
public class ShareableWithPackageShare {
    private long id;
    private ContainerWithPackageShare container;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    public ContainerWithPackageShare getContainerWithPackageShare() {
        return container;
    }

    public void setContainerWithPackageShare(ContainerWithPackageShare container) {
        this.container = container;
    }
}
