/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import static org.mockito.Mockito.mock;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.audit.AuditLogger;

import example.Child;
import example.TestCheckMappings;

public class PersistenceResourceTestSetup extends PersistentResource {

    private static final AuditLogger MOCK_AUDIT_LOGGER = mock(AuditLogger.class);

    protected final ElideSettings elideSettings;

    public PersistenceResourceTestSetup() {
        super(
                new Child(),
                null,
                null, // new request scope + new Child == cannot possibly be a UUID for this object
                new RequestScope(null, null, null, null, null,
                        new ElideSettingsBuilder(null)
                                .withEntityDictionary(new EntityDictionary(TestCheckMappings.MAPPINGS))
                                .withAuditLogger(MOCK_AUDIT_LOGGER)
                                .withDefaultMaxPageSize(10)
                                .withDefaultPageSize(10)
                                .build(),
                        false
                )
        );

        elideSettings = new ElideSettingsBuilder(null)
                .withEntityDictionary(dictionary)
                .withAuditLogger(MOCK_AUDIT_LOGGER)
                .withDefaultMaxPageSize(10)
                .withDefaultPageSize(10)
                .build();
    }
}
