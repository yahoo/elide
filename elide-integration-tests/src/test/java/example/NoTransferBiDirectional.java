/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.NonTransferable;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToOne;

@Entity
@Include(rootLevel = true, type = "noShareBid")
@NonTransferable
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
