/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.ComputedAttribute;
import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.Include;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

/**
 * Used to test computed attributes.
 */
@Entity
@Include
public class User {
    @JsonIgnore
    private long id;

    private String reversedPassword;

    private Set<NoShareEntity> noShares;

    @OneToOne
    public NoShareEntity getNoShare() {
        return noShare;
    }

    public void setNoShare(NoShareEntity noShare) {
        this.noShare = noShare;
    }

    @OneToMany
    public Set<NoShareEntity> getNoShares() {
        return noShares;
    }

    public void setNoShares(Set<NoShareEntity> noShares) {
        this.noShares = noShares;
    }

    private NoShareEntity noShare;

    /**
     * Get password.
     *
     * @return empty string
     */
    @ComputedAttribute
    @Transient
    public String getPassword() {
        return "";
    }


    /**
     * Sets the password but first reverses it.
     * @param password password to encode
     */
    @ComputedAttribute
    @Transient
    public void setPassword(String password) {
        this.reversedPassword = new StringBuilder(password).reverse().toString();
    }

    /**
     * Get reversed password.
     *
     * @return the 'encrypted' password
     */
    @Exclude
    public String getReversedPassword() {
        return reversedPassword;
    }

    /**
     * Sets the password.  This is intended for Hibernate.
     *
     * @param reversedPassword reversed password
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
