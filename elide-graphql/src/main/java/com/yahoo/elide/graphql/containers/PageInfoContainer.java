/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.containers;

import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.request.Pagination;
import com.yahoo.elide.graphql.Environment;
import com.yahoo.elide.graphql.KeyWord;

import lombok.Getter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Container for nodes.
 */
public class PageInfoContainer implements GraphQLContainer {
    @Getter private final ConnectionContainer connectionContainer;

    public PageInfoContainer(ConnectionContainer connectionContainer) {
        this.connectionContainer = connectionContainer;
    }

    @Override
    public Object processFetch(Environment context) {
        String fieldName = context.field.getName();
        ConnectionContainer connectionContainer = getConnectionContainer();
        Optional<Pagination> pagination = connectionContainer.getPagination();

        List<String> ids = connectionContainer.getPersistentResources().stream()
                .map(PersistentResource::getId)
                .sorted()
                .collect(Collectors.toList());

        return pagination.map(pageValue -> {
            switch (KeyWord.byName(fieldName)) {
                case PAGE_INFO_HAS_NEXT_PAGE: {
                    int numResults = ids.size();
                    int nextOffset = numResults + pageValue.getOffset();
                    return nextOffset < pageValue.getPageTotals();
                }
                case PAGE_INFO_START_CURSOR:
                    return pageValue.getOffset();
                case PAGE_INFO_END_CURSOR:
                    return pageValue.getOffset() + ids.size();
                case PAGE_INFO_TOTAL_RECORDS:
                    return pageValue.getPageTotals();
                default:
                    break;
            }
            throw new BadRequestException("Invalid request. Looking for field: "
                    + fieldName + " in an pageInfo object.");
        }).orElseThrow(() -> new BadRequestException("Could not generate pagination information for type: "
                + connectionContainer.getTypeName()));
    }
}
