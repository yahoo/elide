/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.nontransferable;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.NonTransferable;
import example.BaseId;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;

@Entity
@Include(name = "strictNoShare")
@NonTransferable(strict = true)
public class StrictNoTransfer extends BaseId {
    private StrictNoTransfer other;

    @OneToOne(fetch = FetchType.LAZY)
    public StrictNoTransfer getOther() {
        return other;
    }

    public void setOther(StrictNoTransfer other) {
        this.other = other;
    }
}
