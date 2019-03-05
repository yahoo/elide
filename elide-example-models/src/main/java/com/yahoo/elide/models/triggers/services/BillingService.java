/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.models.triggers.services;

import com.yahoo.elide.models.triggers.Invoice;

/**
 * Interface to make a financial transaction based on an Invoice.
 */
public interface BillingService {

    /**
     * Purchases the items in the invoice.
     * @param invoice Contains a list of what to purchase
     * @return surcharges to add to the total.
     */
    public long purchase(Invoice invoice);
}
