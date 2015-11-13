/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Include(rootLevel = true, type = "stringId")
@Entity
public class FieldAnnotations {

    private Long id;

    @Column
    public long publicField;

    private Boolean privateField;

    @Id
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
