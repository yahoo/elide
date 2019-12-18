/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.standalone.checks;

import com.yahoo.elide.annotation.SecurityCheck;
import com.yahoo.elide.security.RequestScope;
import com.yahoo.elide.security.checks.OperationCheck;

import java.util.Optional;

@SecurityCheck(AdminCheck.USER_IS_ADMIN)
public class AdminCheck extends OperationCheck {

    public static final String USER_IS_ADMIN = "User is Admin";

    @Override
    public boolean ok(Object object, RequestScope requestScope, Optional optional) {

        //There are no admins...
        return false;
    }
}
