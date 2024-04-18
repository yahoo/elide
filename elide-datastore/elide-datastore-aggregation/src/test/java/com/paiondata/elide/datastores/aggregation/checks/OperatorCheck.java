/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.checks;

import com.paiondata.elide.annotation.SecurityCheck;
import com.paiondata.elide.core.security.checks.prefab.Role.RoleMemberCheck;

@SecurityCheck(OperatorCheck.OPERTOR_CHECK)
public class OperatorCheck extends RoleMemberCheck {

    public static final String OPERTOR_CHECK = "operator";

    public OperatorCheck() {
        super("operator");
    }
}
