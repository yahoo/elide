/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.models.jpa;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.UpdatePermission;
import example.checks.AdminCheck;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

@Include(name = "group")
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
