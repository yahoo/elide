/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.Include;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
@Include(name = "chapter")
/**
 * This class tests using JPA Field based access.
 */
public class Chapter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Exclude
    private String naturalKey = UUID.randomUUID().toString();

    @Override
    public int hashCode() {
        return naturalKey.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Chapter && ((Chapter) obj).naturalKey.equals(naturalKey);
    }

    @Getter @Setter private String title;
}
