/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yahoo.elide.annotation.ComputedProperty;
import com.yahoo.elide.annotation.Include;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;


/**
 * Used to test computed properties.
 */
@Entity
@Include(rootLevel = true)
public class User {
    @JsonIgnore
    private long id;

    private String reversedPassword;

    /**
     * @return empty string
     */
    @ComputedProperty
    @Transient
    public String getPassword() {
        return "";
    }


    /**
     * Sets the password but first reverses it.
     */
    @ComputedProperty
    @Transient
    public void setPassword(String password) {
        this.reversedPassword = new StringBuilder(password).reverse().toString();
    }

    /**
     * @return the 'encrypted' password
     */
    public String getReversedPassword() {
        return reversedPassword;
    }

    /**
     * Sets the password.  This is intended for Hibernate
     * @param reversedPassword
     */
    public void setReversedPassword(String reversedPassword) {
        this.reversedPassword = reversedPassword;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
