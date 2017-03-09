/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.security.User;
import lombok.Getter;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The equivalent of {@link com.yahoo.elide.core.RequestScope} for GraphQL.
 */
public class GraphQLContext {
    @Getter private final DataStoreTransaction transaction;
    @Getter private final User user;
    @Getter private final EntityDictionary dictionary;
    @Getter private final Set<PersistentResource> newPersistentResources;
    @Getter private final LinkedHashSet<PersistentResource> dirtyResources;

    public GraphQLContext(DataStoreTransaction transaction,
                          User user,
                          EntityDictionary dictionary,
                          Set<PersistentResource> newPersistentResources,
                          LinkedHashSet<PersistentResource> dirtyResources) {
        this.transaction = transaction;
        this.user = user;
        this.dictionary = dictionary;
        this.newPersistentResources = newPersistentResources;
        this.dirtyResources = dirtyResources;
    }
}
