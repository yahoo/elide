/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.config;

import lombok.Data;

/**
 * Extra properties for setting up GraphQL subscription support.
 */
@Data
public class SubscriptionProperties {

    /**
     * Turns on/off GraphQL subscriptions.
     */
    private boolean enabled = false;

    private boolean sendPingOnSubscribe = false;
}
