/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.models.generics;

import com.yahoo.elide.models.BaseId;

import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import java.util.Set;

@MappedSuperclass
public class Person<T, S> extends BaseId {

    @OneToOne
    T boss;

    @OneToMany
    Set<S> reports;
}
