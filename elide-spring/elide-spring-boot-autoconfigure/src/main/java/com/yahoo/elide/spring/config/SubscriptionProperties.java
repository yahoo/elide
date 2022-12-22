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
public class SubscriptionProperties extends ControllerProperties {

    /**
     * Whether Elide should publish subscription notifications to JMS on lifecycle events.
     */
    protected boolean publishingEnabled = isEnabled();

    /**
     * Websocket sends a PING immediate after receiving a SUBSCRIBE.  Only useful for testing.
     * @see com.yahoo.elide.datastores.jms.websocket.SubscriptionWebSocketTestClient
     */
    protected boolean sendPingOnSubscribe = false;

    /**
     * Time allowed in milliseconds from web socket creation to successfully receiving a CONNECTION_INIT message.
     */
    protected int connectionTimeoutMs = 5000;

    /**
     * Maximum number of outstanding GraphQL queries per websocket.
     */
    protected int maxSubscriptions = 30;

    /**
     * Maximum message size that can be sent to the websocket.
     */
    protected int maxMessageSize = 10000;

    /**
     * Maximum idle timeout in milliseconds with no websocket activity.
     */
    protected long idleTimeoutMs = 300000;
}
