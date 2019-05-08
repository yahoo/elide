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

/**
 * A test entity that has an ID field whose field name is not "id".
 */
@Include(rootLevel = true)
@Entity
public class IdFieldAnnotations {

    @Id
    private Long surrogateKey;

    @Column
    public long publicField;

    private Boolean privateField;

    @OneToMany(mappedBy = "parent")
    private Set<IdFieldAnnotations> children;

    @ManyToOne
    private IdFieldAnnotations parent;

    public Long getSurrogateKey() {
        return surrogateKey;
    }

    public void setSurrogateKey(Long surrogateKey) {
        this.surrogateKey = surrogateKey;
    }

    @Column
    public Boolean getPrivateField() {
        return privateField;
    }

    public void setPrivateField(Boolean privateField) {
        this.privateField = privateField;
    }
}
