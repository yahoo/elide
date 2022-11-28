/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.NonTransferable;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.Collection;

/**
 * Container for shareables and unshareables.
 */
@Entity
@Table(name = "container")
@Include(name = "container")
@NonTransferable
public class Container extends BaseId {
    private Collection<Untransferable> untransferables;
    private Collection<Transferable> transferables;

    @OneToMany(mappedBy = "container")
    public Collection<Untransferable> getUntransferables() {
        return untransferables;
    }

    public void setUntransferables(Collection<Untransferable> untransferables) {
        this.untransferables = untransferables;
    }

    @OneToMany(mappedBy = "container")
    public Collection<Transferable> getTransferables() {
        return transferables;
    }

    public void setTransferables(Collection<Transferable> transferables) {
        this.transferables = transferables;
    }
}
