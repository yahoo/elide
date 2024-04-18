/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.jms.websocket;

import com.paiondata.elide.datastores.jms.websocket.SubscriptionWebSocketConfigurator.SubscriptionWebSocketConfiguratorBuilder;

/**
 * Used to customize the mutable {@link SubscriptionWebSocketConfiguratorBuilder}.
 */
public interface SubscriptionWebSocketConfiguratorBuilderCustomizer {
    public void customize(SubscriptionWebSocketConfiguratorBuilder builder);
}
