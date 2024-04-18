/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.graphql.federation;

import java.util.Arrays;

/**
 * The Federation Specification Version.
 */
public enum FederationVersion {
    FEDERATION_2_5("2.5"),
    FEDERATION_2_4("2.4"),
    FEDERATION_2_3("2.3"),
    FEDERATION_2_2("2.2"),
    FEDERATION_2_1("2.1"),
    FEDERATION_2_0("2.0"),
    FEDERATION_1_1("1.1"),
    FEDERATION_1_0("1.0");

    private final String value;

    FederationVersion(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public float floatValue() {
        return Float.valueOf(this.value);
    }

    public int intValue() {
        return Integer.valueOf(this.value.replace(".", ""));
    }

    public static FederationVersion from(String version) {
        return Arrays.stream(FederationVersion.values()).filter(v -> version.equals(v.getValue())).findFirst()
                .orElseThrow(() -> {
                    throw new IllegalArgumentException("Invalid Federation version.");
                });
    }
}
