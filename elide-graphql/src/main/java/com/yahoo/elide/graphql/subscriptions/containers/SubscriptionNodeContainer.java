/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.subscriptions.containers;

import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.dictionary.RelationshipType;
import com.yahoo.elide.core.request.Relationship;
import com.yahoo.elide.graphql.Environment;
import com.yahoo.elide.graphql.containers.NodeContainer;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Container for subscription nodes.
 */
public class SubscriptionNodeContainer extends NodeContainer {

    public SubscriptionNodeContainer(PersistentResource persistentResource) {
        super(persistentResource);
    }

    @Override
    protected Object fetchRelationship(Environment context, Relationship relationship) {
        RelationshipType type = context.parentResource.getRelationshipType(relationship.getName());

        if (type.isToOne()) {
            Set<PersistentResource> resources = (Set<PersistentResource>) context.parentResource
                    .getRelationCheckedFiltered(relationship)
                    .toList(LinkedHashSet::new).blockingGet();
            if (resources.size() > 0) {
                return new SubscriptionNodeContainer(resources.iterator().next());
            } else {
                return null;
            }
        } else {
            Set<PersistentResource> resources = (Set<PersistentResource>) context.parentResource
                    .getRelationCheckedFiltered(relationship)
                    .toList(LinkedHashSet::new).blockingGet();

            return resources.stream()
                    .map(SubscriptionNodeContainer::new)
                    .collect(Collectors.toList());
        }
    }
}
