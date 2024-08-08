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
public class PageInfoContainer implements GraphQLContainer<Object> {
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

        Pagination pageValue = pagination.orElseThrow(() -> new BadRequestException(
                "Could not generate pagination information for type: " + connectionContainer.getTypeName()));

        switch (KeyWord.byName(fieldName)) {
        case PAGE_INFO_HAS_PREVIOUS_PAGE: {
            if (pageValue.getHasPreviousPage() != null) {
                return pageValue.getHasPreviousPage();
            }
            if (pageValue.getDirection() != null) { // cursor
                return null; // if cannot efficiently determine return null
            }
            return pageValue.getOffset() > 0; // offset
        }
        case PAGE_INFO_HAS_NEXT_PAGE: {
            if (pageValue.getHasNextPage() != null) {
                return pageValue.getHasNextPage();
            }
            if (pageValue.getDirection() != null) { // cursor
                return null; // if cannot efficiently determine return null
            }
            int numResults = ids.size();
            int nextOffset = numResults + pageValue.getOffset();
            if (pageValue.getPageTotals() == null) {
                throw new BadRequestException("Cannot determine hasNextPage without totalRecords.");
            }
            return nextOffset < pageValue.getPageTotals(); // offset
        }
        case PAGE_INFO_START_CURSOR:
            if (pageValue.getStartCursor() != null) {
                return pageValue.getStartCursor();
            }
            if (pageValue.getDirection() != null) { // cursor
                return null; // can be null if there are no results
            }
            return pageValue.getOffset(); // offset
        case PAGE_INFO_END_CURSOR:
            if (pageValue.getEndCursor() != null) {
                return pageValue.getEndCursor();
            }
            if (pageValue.getDirection() != null) { // cursor
                return null; // can be null if there are no results
            }
            return pageValue.getOffset() + ids.size(); // offset
        case PAGE_INFO_TOTAL_RECORDS:
            return pageValue.getPageTotals();
        default:
            break;
        }
        throw new BadRequestException("Invalid request. Looking for field: " + fieldName + " in an pageInfo object.");
    }
}
