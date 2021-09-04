package com.yahoo.elide.extension.test.models;

import com.yahoo.elide.annotation.Include;

import javax.persistence.Entity;
import javax.persistence.Id;

@Include
@Entity
public class Book {
    @Id
    private long id;

    private String title;
}
