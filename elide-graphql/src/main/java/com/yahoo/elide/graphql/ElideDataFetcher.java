/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.Relationship;
import com.yahoo.elide.graphql.containers.GraphQLContainer;
import lombok.Getter;

import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;

public abstract class ElideDataFetcher {
    @Getter
    NonEntityDictionary nonEntityDictionary;

    public ElideDataFetcher(NonEntityDictionary nonEntityDictionary) {
        this.nonEntityDictionary = nonEntityDictionary;
    }

    /**
     * Fetches a relationship for a top-level entity.
     *
     * @param parentResource Parent object
     * @param relationship   constructed relationship object with entityProjection
     * @param ids            List of ids
     * @return persistence resource object(s)
     */
    public abstract GraphQLContainer fetchRelationship(
            PersistentResource<?> parentResource,
            @NotNull Relationship relationship,
            Optional<List<String>> ids);

    /**
     * Fetches a root-level entity.
     * @param requestScope Request scope
     * @param projection constructed entityProjection for a class
     * @param ids List of ids (can be NULL)
     * @return {@link PersistentResource} object(s)
     */
    public abstract GraphQLContainer fetchObject(
            RequestScope requestScope,
            EntityProjection projection,
            Optional<List<String>> ids
    );
}
