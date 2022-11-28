/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.dynamic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.yahoo.elide.annotation.ApiVersion;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.modelconfig.model.NamespaceConfig;
import org.junit.jupiter.api.Test;

public class NamespacePackageTest {

    @Test
    void testAnnotations() throws Exception {
        NamespaceConfig testNamespace = NamespaceConfig.builder()
                .description("A test Namespace")
                .name("Test")
                .build();

        NamespacePackage namespace = new NamespacePackage(testNamespace);

        ReadPermission readPermission = namespace.getDeclaredAnnotation(ReadPermission.class);
        assertEquals("Prefab.Role.All", readPermission.expression());

        Include meta = namespace.getDeclaredAnnotation(Include.class);
        assertEquals("A test Namespace", meta.description());
        assertNull(meta.friendlyName());

        ApiVersion apiVersion = namespace.getDeclaredAnnotation(ApiVersion.class);
        assertEquals("", apiVersion.version());
    }

    @Test
    void testGet() throws Exception {
        NamespaceConfig testNamespace = NamespaceConfig.builder()
                .description("A test Namespace")
                .name("Test")
                .build();

        NamespacePackage namespace = new NamespacePackage(testNamespace);
        assertEquals("Test", namespace.getName());
    }
}
