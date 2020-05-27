/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

import lombok.Getter;

import java.util.UUID;

public abstract class DataStoreTransactionImplementation {
    @Getter private final UUID requestId = UUID.randomUUID();

    public UUID getRequestId() {
        return requestId;
    }
}
