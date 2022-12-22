/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Audit;
import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.Include;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

/**
 * Model for authors.
 */
@Entity
@Table(name = "author")
@Include
@Audit(action = Audit.Action.CREATE,
        operation = 10,
        logStatement = "{0}",
        logExpressions = {"${author.name}"})
public class Author {
    public enum AuthorType {
        EXCLUSIVE,
        CONTRACTED,
        FREELANCE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter @Setter
    private Long id;

    @Exclude
    private String naturalKey = UUID.randomUUID().toString();

    @Override
    public int hashCode() {
        return naturalKey.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Author && naturalKey.equals(((Author) obj).naturalKey);
    }

    @Getter @Setter
    private String name;

    @ManyToMany(mappedBy = "authors")
    @Getter @Setter
    private Collection<Book> books = new ArrayList<>();

    @ManyToMany(targetEntity = Book.class, mappedBy = "authors")
    @Getter @Setter
    private Collection<Product> products = new ArrayList<>();

    @Getter @Setter
    private AuthorType type;

    @Getter @Setter
    private Address homeAddress;

    @Getter @Setter
    private Set<Address> vacationHomes;

    @Getter @Setter
    private Map<Object, Object> stuff;

    @ElementCollection
    @Getter @Setter
    private Collection<String> awards;
}
