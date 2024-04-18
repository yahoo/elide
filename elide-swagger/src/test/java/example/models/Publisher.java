/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.models;

import com.paiondata.elide.annotation.Include;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

@Entity
@Include(description = "Publisher information.", friendlyName = "Publisher Title")
public class Publisher {

    /* Tests an inner class */
    public static class Address {
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

    @Schema(description = "Phone number", example = "555-000-1111")
    public String phone;

    /* Test for custom serde */
    @Schema(description = "Time Zone")
    public TimeZone timeZone;
}
