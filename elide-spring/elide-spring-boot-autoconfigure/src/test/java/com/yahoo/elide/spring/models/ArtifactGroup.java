/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.models;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.spring.checks.AdminCheck;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

@Include(rootLevel = true, type = "group")
@Entity
@Data
public class ArtifactGroup {
    @Id
    private String name = "";

    private String commonName = "";

    private String description = "";

    @CreatePermission(expression = AdminCheck.USER_IS_ADMIN)
    @UpdatePermission(expression = AdminCheck.USER_IS_ADMIN)
    private boolean deprecated = false;

    @OneToMany(mappedBy = "group")
    private List<ArtifactProduct> products = new ArrayList<>();
}
