/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example.generics;

import example.BaseId;

import javax.persistence.MappedSuperclass;
import javax.persistence.OneToOne;


/**
 * Parameterized base class for testing.
 * @param <T> Boss Type
 */
@MappedSuperclass
public class Peon<T> extends BaseId {

    @OneToOne
    public T boss;
}
