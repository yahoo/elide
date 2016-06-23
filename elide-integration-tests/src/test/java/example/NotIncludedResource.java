/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.security.checks.prefab.Role;

import javax.persistence.*;

@ReadPermission(any = {Role.ALL.class})
@CreatePermission(any = {Role.ALL.class})
@UpdatePermission(any = {Role.ALL.class})
@Entity
public class NotIncludedResource {
    private Long id;
    private String someParams;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSomeParams() {
        return someParams;
    }

    public void setSomeParams(String someParams) {
        this.someParams = someParams;
    }
}
