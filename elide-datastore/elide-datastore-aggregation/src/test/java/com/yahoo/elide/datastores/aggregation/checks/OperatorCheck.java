/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.checks;

import com.yahoo.elide.annotation.SecurityCheck;
import com.yahoo.elide.core.security.checks.prefab.Role.RoleMemberCheck;

@SecurityCheck(OperatorCheck.OPERTOR_CHECK)
public class OperatorCheck extends RoleMemberCheck {

    public static final String OPERTOR_CHECK = "operator";

    public OperatorCheck() {
        super("operator");
    }
}
