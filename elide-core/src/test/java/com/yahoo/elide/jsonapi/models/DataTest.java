/*
 * Copyright 2024, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.core.dictionary.RelationshipType;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Test for Data.
 */
class DataTest {
    @Test
    void getSingleValueNullShouldNotThrow() {
        Data<Resource> data = new Data<Resource>((Resource) null);
        assertNull(data.getSingleValue());
    }

    @Test
    void getSingleValueShouldNotThrow() {
        Resource resource = new Resource("book", "1");
        Data<Resource> data = new Data<Resource>(resource);
        assertEquals(resource, data.getSingleValue());
    }

    @Test
    void getSingleValueCollectionShouldThrow() {
        Data<Resource> data = new Data<>(List.of(new Resource("book", "1"), new Resource("book", "2")));
        assertThrows(IllegalAccessError.class, () -> data.getSingleValue());
    }

    @Test
    void getNullShouldNotThrow() {
        Data<Resource> data = new Data<Resource>((Resource) null);
        assertTrue(data.get().isEmpty());
    }

    @Test
    void getSingleValueCollectionAsToOneShouldThrow() {
        Data<Resource> data = new Data<>(List.of(new Resource("book", "1"), new Resource("book", "2")),
                RelationshipType.MANY_TO_ONE);
        assertThrows(IndexOutOfBoundsException.class, () -> data.getSingleValue());
    }

    @Test
    void sort() {
        Data<Resource> data = new Data<>(List.of(new Resource("book", "1"), new Resource("book", "2")));
        data.sort((left, right) -> {
            return right.getId().compareTo(left.getId());
        });
        assertEquals("2", data.get().iterator().next().getId());
    }

    @Test
    void flux() {
        Data<Resource> data = new Data<>(Flux.just(new Resource("book", "1"), new Resource("book", "2")));
        List<Resource> resource = data.get().stream().toList();
        assertEquals(2, resource.size());
        assertEquals("1", resource.get(0).getId());
        assertEquals("2", resource.get(1).getId());
    }

    @Test
    void toResourceIdentifiers() {
        Data<Resource> data = new Data<>(Flux.just(new Resource("book", "1"), new Resource("book", "2")));
        List<ResourceIdentifier> identifiers = data.toResourceIdentifiers().stream().toList();
        assertEquals(2, identifiers.size());
        assertEquals("1", identifiers.get(0).getId());
        assertEquals("2", identifiers.get(1).getId());

        identifiers = data.toResourceIdentifiers().stream().toList();
        assertEquals(2, identifiers.size());
        assertEquals("1", identifiers.get(0).getId());
        assertEquals("2", identifiers.get(1).getId());
    }
}
