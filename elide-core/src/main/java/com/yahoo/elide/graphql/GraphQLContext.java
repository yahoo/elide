/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.security.User;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The equivalent of {@link com.yahoo.elide.core.RequestScope} for GraphQL.
 */
public class GraphQLContext {
    public final RequestScope requestScope;
    @Getter @Setter private Object lastLoaded;

    public GraphQLContext(RequestScope requestScope) {
        this.requestScope = requestScope;
        this.lastLoaded = null;
    }
}
