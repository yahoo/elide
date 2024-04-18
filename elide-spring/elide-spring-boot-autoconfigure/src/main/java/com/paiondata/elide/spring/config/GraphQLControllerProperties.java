/*
 * Copyright 2022, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.config;


import org.springframework.boot.context.properties.NestedConfigurationProperty;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Arrays;

/**
 * Extra controller properties for the GraphQL endpoint.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GraphQLControllerProperties extends ControllerProperties {

    @Data
    public static class Federation {
        /**
         * The Federation Specification Version.
         */
        public enum Version {
            FEDERATION_2_5("2.5"),
            FEDERATION_2_4("2.4"),
            FEDERATION_2_3("2.3"),
            FEDERATION_2_2("2.2"),
            FEDERATION_2_1("2.1"),
            FEDERATION_2_0("2.0"),
            FEDERATION_1_1("1.1"),
            FEDERATION_1_0("1.0");

            private final String value;

            Version(String value) {
                this.value = value;
            }

            public String getValue() {
                return this.value;
            }

            public static Version from(String version) {
                return Arrays.stream(Version.values()).filter(v -> version.equals(v.getValue())).findFirst()
                        .orElseThrow(() -> {
                            throw new IllegalArgumentException("Invalid Federation version.");
                        });
            }
        }

        /**
         * Turns on/off Apollo federation schema.
         */
        private boolean enabled = false;

        /**
         * The Federation Specification Version to generate.
         */
        private Version version = Version.FEDERATION_1_0;
    }

    /**
     * Settings for subscriptions.
     */
    @NestedConfigurationProperty
    private SubscriptionProperties subscription = new SubscriptionProperties();

    private Federation federation = new Federation();
}
