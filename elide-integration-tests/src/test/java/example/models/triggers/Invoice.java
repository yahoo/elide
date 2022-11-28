/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example.models.triggers;

import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.Include;
import example.models.BaseId;
import example.models.triggers.services.BillingService;

import jakarta.persistence.Entity;
import jakarta.persistence.Transient;
import lombok.Data;

import javax.inject.Inject;

/**
 * Invoice for a group of purchased items.
 */
@Entity(name = "customerInvoice")
@Include(name = "customerInvoice")
@Data
public class Invoice extends BaseId {
    @Transient
    @Exclude
    @Inject
    private BillingService billingService;

    private boolean complete = false;
    private long total = 0;
}
