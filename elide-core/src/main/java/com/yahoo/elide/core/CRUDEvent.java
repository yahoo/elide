/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core;

import com.yahoo.elide.security.ChangeSpec;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Optional;

/**
 * Captures all the bits related to a CRUD operation on a model.
 */
@Data
@AllArgsConstructor
public class CRUDEvent {
    private CRUDAction eventType;
    private PersistentResource resource;
    private String fieldName;
    private Optional<ChangeSpec> changes;

    public boolean isCreateEvent() {
        return eventType == CRUDAction.CREATE;
    }

    public boolean isUpdateEvent() {
        return eventType == CRUDAction.UPDATE;
    }

    public boolean isDeleteEvent() {
        return eventType == CRUDAction.DELETE;
    }

    public boolean isReadEvent() {
        return eventType == CRUDAction.READ;
    }

    /**
     * Enum describing possible CRUD actions.
     */
    public static enum CRUDAction {
        CREATE, READ, UPDATE, DELETE
    }
}
