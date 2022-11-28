/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.nontransferable;

import com.yahoo.elide.annotation.Include;
import example.BaseId;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;

@Entity
@Include(name = "noShareBid")
public class NoTransferBiDirectional extends BaseId {
    private NoTransferBiDirectional other;

    @OneToOne(fetch = FetchType.LAZY)
    public NoTransferBiDirectional getOther() {
        return other;
    }

    public void setOther(NoTransferBiDirectional other) {
        this.other = other;
    }
}
