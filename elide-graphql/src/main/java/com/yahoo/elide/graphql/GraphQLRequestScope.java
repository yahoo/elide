/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.security.User;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MultivaluedHashMap;

/**
 * Persistent state across GraphQL requests. This object is stored in the environment context.
 */
public class GraphQLRequestScope extends RequestScope {
    @Getter private final Map<String, Long> totalRecordCounts = new HashMap<>();

    public GraphQLRequestScope(DataStoreTransaction transaction,
                               User user,
                               ElideSettings elideSettings) {
        // TODO: We're going to break out the two request scopes. `RequestScope` should become an interface and
        // we should have a GraphQLRequestScope and a JSONAPIRequestScope.
        // TODO: What should mutate multiple entity value be? There is a problem with this setting in practice.
        // Namely, we don't filter or paginate in the data store.
        super("/", null, transaction, user, new MultivaluedHashMap<>(), elideSettings, false);
    }
}
