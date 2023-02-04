/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package nocreate;

import com.yahoo.elide.annotation.Include;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Must inherit @CreatePermission from package.  Do not add here.
 */
@Include(name = "nocreate") // optional here because class has this name
@Entity
@Table(name = "nocreate")
public class NoCreateEntity {
    private long id;

    @Id
    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
}
