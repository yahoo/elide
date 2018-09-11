/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

@Include(rootLevel = true, type = "stringId")
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
