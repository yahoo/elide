/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.security.Role;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@CreatePermission(any = {Role.ALL.class })
@ReadPermission(all = {Role.NONE.class })
@Include(rootLevel = true)
@Entity
public class YetAnotherPermission {
    private Long id;
    private String hiddenName;
    private String youShouldBeAbleToRead;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHiddenName() {
        return hiddenName;
    }

    public void setHiddenName(String hiddenName) {
        this.hiddenName = hiddenName;
    }

    @ReadPermission(any = {Role.ALL.class})
    public String getYouShouldBeAbleToRead() {
        return youShouldBeAbleToRead;
    }

    public void setYouShouldBeAbleToRead(String youShouldBeAbleToRead) {
        this.youShouldBeAbleToRead = youShouldBeAbleToRead;
    }
}
