/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.SharePermission;
import com.yahoo.elide.security.checks.prefab.Role;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import java.util.Collection;

@Entity
@Include(rootLevel = true, type = "chapter")
@SharePermission(all = {Role.ALL.class})
@Setter
public class Chapter {
    private Long id;
    @Getter private String title;

    private Collection<Book> books;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long getId() {
        return id;
    }

    @ManyToMany(mappedBy = "chapters")
    public Collection<Book> getBooks() {
        return books;
    }
}
