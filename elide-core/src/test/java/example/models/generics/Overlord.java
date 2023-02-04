/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example.models.generics;

import example.models.BaseId;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;

import java.util.Set;


/**
 * Parameterized base class for testing.
 * @param <T> Minion type
 */
@MappedSuperclass
public class Overlord<T> extends BaseId {

    @OneToMany
    private Set<T> minions;
}
