/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.config;

import org.springframework.boot.convert.DurationUnit;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Extra properties for setting up GraphQL subscription support.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SubscriptionProperties extends ControllerProperties {

    @Data
    public static class Publishing {
        /**
         * Whether Elide should publish subscription notifications to JMS on lifecycle events.
         */
        private boolean enabled = true;
    }

    protected Publishing publishing = new Publishing();

    /**
     * Websocket sends a PING immediate after receiving a SUBSCRIBE.  Only useful for testing.
     * @see com.paiondata.elide.datastores.jms.websocket.SubscriptionWebSocketTestClient
     */
    protected boolean sendPingOnSubscribe = false;

    /**
     * Time allowed in milliseconds from web socket creation to successfully receiving a CONNECTION_INIT message.
     */
    @DurationUnit(ChronoUnit.MILLIS)
    protected Duration connectionTimeout = Duration.ofMillis(5000L);

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
    @DurationUnit(ChronoUnit.MILLIS)
    protected Duration idleTimeout = Duration.ofMillis(300000L);
}
