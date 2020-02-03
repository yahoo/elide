/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * A shareable bean.
 */
@Entity
@Table(name = "transferable")
@Include(rootLevel = true, type = "transferable")
public class Transferable extends BaseId {
    private Container container;

    @ManyToOne(fetch = FetchType.LAZY)
    public Container getContainer() {
        return container;
    }

    public void setContainer(Container container) {
        this.container = container;
    }
}
