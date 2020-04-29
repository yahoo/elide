/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example.models.triggers;

import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.LifeCycleHookBinding;
import example.models.BaseId;
import example.models.triggers.services.BillingService;

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

    @LifeCycleHookBinding(operation = LifeCycleHookBinding.Operation.CREATE, hook = InvoiceCompletionHook.class,
            phase = LifeCycleHookBinding.TransactionPhase.PRECOMMIT)
    @LifeCycleHookBinding(operation = LifeCycleHookBinding.Operation.UPDATE, hook = InvoiceCompletionHook.class,
            phase = LifeCycleHookBinding.TransactionPhase.PRECOMMIT)
    private boolean complete = false;
    private long total = 0;
}
