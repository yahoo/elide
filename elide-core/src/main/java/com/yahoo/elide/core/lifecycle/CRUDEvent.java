/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.lifecycle;

import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.CREATE;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.DELETE;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.UPDATE;

import com.yahoo.elide.annotation.LifeCycleHookBinding;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.PersistentResource;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Optional;

/**
 * Captures all the bits related to a CRUD operation on a model.
 */
@Data
@AllArgsConstructor
public class CRUDEvent {
    private LifeCycleHookBinding.Operation eventType;
    private PersistentResource resource;
    private String fieldName;
    private Optional<ChangeSpec> changes;

    public boolean isCreateEvent() {
        return eventType == CREATE;
    }

    public boolean isUpdateEvent() {
        return eventType == UPDATE;
    }

    public boolean isDeleteEvent() {
        return eventType == DELETE;
    }
}
