/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.swagger.models;

import com.yahoo.elide.annotation.Include;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.util.Map;
import java.util.Set;

@Entity
@Include(rootLevel = true)
public class Publisher {

    /* Tests an inner class */
    public class Address {
        public String street;
        public String city;
        public String state;
        public String zip;
    }

    @OneToMany
    public Set<Book> getBooks() {
        return null;
    }

    @OneToMany
    public Set<Author> getExclusiveAuthors() {
        return null;
    }

    public Address billingAddress;

    public Map<String, Integer> billingCodes;
}
