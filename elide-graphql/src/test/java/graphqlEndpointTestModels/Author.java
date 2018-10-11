/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package graphqlEndpointTestModels;

import com.yahoo.elide.annotation.ComputedAttribute;
import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.SharePermission;
import com.yahoo.elide.annotation.UpdatePermission;

import graphqlEndpointTestModels.security.UserChecks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

@Include
@Entity
@CreatePermission(expression = Author.PERMISSION)
@ReadPermission(expression = Author.PERMISSION)
@UpdatePermission(expression = Author.PERMISSION)
@DeletePermission(expression = Author.PERMISSION)
@SharePermission
public class Author {
    Long id;
    String name;
    Set<Book> books = new HashSet<>();
    DisallowShare noShare;
    Map<String, String> bookTitlesAndAwards = new HashMap<>();

    public static final String PERMISSION = UserChecks.IS_USER_1 + " OR " + UserChecks.IS_USER_2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ManyToMany
    public Set<Book> getBooks() {
        return books;
    }

    public void setBooks(Set<Book> books) {
        this.books = books;
    }

    @Transient
    @ComputedAttribute
    public int getBookCount() {
        return getBooks().size();
    }

    public void setBookCount(int unused) {
        // Do nothing
    }

    @OneToOne
    public DisallowShare getNoShare() {
        return noShare;
    }

    public void setNoShare(DisallowShare noShare) {
        this.noShare = noShare;
    }

    public Map<String, String> getBookTitlesAndAwards() {
        return bookTitlesAndAwards;
    }

    public void setBookTitlesAndAwards(Map<String, String> bookTitlesAndAwards) {
        this.bookTitlesAndAwards = bookTitlesAndAwards;
    }
}
