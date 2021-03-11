/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.nontransferable;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.NonTransferable;
import example.BaseId;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToOne;

@Entity
@Include(type = "strictNoShare")
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
