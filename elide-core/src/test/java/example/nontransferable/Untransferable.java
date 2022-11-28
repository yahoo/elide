/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.nontransferable;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 * Entity level Unshareable bean.
 */
@Entity
public class Untransferable {
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
