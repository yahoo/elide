/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.models.generics;

import com.yahoo.elide.models.BaseId;

import javax.persistence.MappedSuperclass;
import javax.persistence.OneToOne;


/**
 * Parameterized base class for testing.
 * @param <T> Boss Type
 */
@MappedSuperclass
public class Peon<T> extends BaseId {

    @OneToOne
    private T boss;
}
