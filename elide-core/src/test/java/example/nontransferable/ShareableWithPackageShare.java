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
import jakarta.persistence.ManyToOne;

/**
 * Package level shareable bean.
 */
@Entity
@NonTransferable(enabled = false)
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
