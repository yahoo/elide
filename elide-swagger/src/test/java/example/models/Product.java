/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.models;

import com.paiondata.elide.annotation.CreatePermission;
import com.paiondata.elide.annotation.DeletePermission;
import com.paiondata.elide.annotation.Include;
import com.paiondata.elide.core.security.checks.prefab.Role;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

/**
 * Product.
 */
@Entity
@Include(friendlyName = "Product")
@CreatePermission(expression = Role.NONE_ROLE)
@DeletePermission(expression = Role.NONE_ROLE)
@Getter
@Setter
public class Product {
    String id;
    String sku;
    String packageName;
}
