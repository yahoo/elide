/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.swagger.models;

import com.yahoo.elide.annotation.Include;

import java.util.Map;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.OneToMany;

@Entity
@Include(rootLevel = true)
public class Publisher {

    /* Tests an inner class */
    public class Address {
        public String street;
        public String city;
        public String state;
        public String zip;

        /*
         * Adding an Elide entity as an attribute is a test case for making sure the Swagger ModelResolver creates
         * a ref back to our existing model for Book (rather than create a new model).
         */
        public Book hmmm;
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
