/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
/**
 * Models Package.
 */
@ReadPermission(expression = "Prefab.Role.All")
@UpdatePermission(expression = "Prefab.Role.None")
@DeletePermission(expression = "Prefab.Role.None")
@CreatePermission(expression = "Prefab.Role.None")
package com.yahoo.elide.datastores.aggregation.metadata.models;
import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;
