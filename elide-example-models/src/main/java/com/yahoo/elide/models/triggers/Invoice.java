/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.models.triggers;

import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.OnCreatePreCommit;
import com.yahoo.elide.annotation.OnUpdatePreCommit;
import com.yahoo.elide.models.BaseId;
import com.yahoo.elide.models.triggers.services.BillingService;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.RequestScope;

import lombok.Data;

import javax.inject.Inject;
import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * Invoice for a group of purchased items.
 */
@Entity(name = "customerInvoice")
@Include(rootLevel = true, type = "customerInvoice")
@Data
public class Invoice extends BaseId {
    @Transient
    @Exclude
    @Inject
    private BillingService billingService;

    private boolean complete = false;
    private long total = 0;

    @OnCreatePreCommit("complete")
    @OnUpdatePreCommit("complete")
    public void onComplete(RequestScope scope, ChangeSpec changes) {
        boolean after = (Boolean) changes.getModified();
        boolean before = (Boolean) changes.getOriginal();

        if (after == before) {
            return;
        }

        if (after == true) {
            long surcharge = billingService.purchase(this);
            setTotal(total + surcharge);
        }
    }
}
