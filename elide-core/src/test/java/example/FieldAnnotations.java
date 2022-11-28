/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import java.util.Set;

@Include(name = "stringId")
@Entity
public class FieldAnnotations {

    @Id
    private Long id;

    @Column
    public long publicField;

    private Boolean privateField;

    @OneToMany(mappedBy = "parent")
    private Set<FieldAnnotations> children;

    @ManyToOne
    private FieldAnnotations parent;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Column
    public Boolean getPrivateField() {
        return privateField;
    }

    public void setPrivateField(Boolean privateField) {
        this.privateField = privateField;
    }
}
