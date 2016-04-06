/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
@CreatePermission(all = { Role.NONE.class })
package nocreate;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.security.checks.prefab.Role;
