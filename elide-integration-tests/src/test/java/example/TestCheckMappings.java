/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.security.checks.Check;
import com.yahoo.elide.security.checks.prefab.Common;
import com.yahoo.elide.security.checks.prefab.Role;

import com.google.common.collect.ImmutableMap;

import java.util.HashMap;

/**
 * Check mappings to be used by tests.
 */
public class TestCheckMappings {
    public static final HashMap<String, Class<? extends Check>> MAPPINGS =
            new HashMap<>(ImmutableMap.<String, Class<? extends Check>>builder()
                    .put("allow all", Role.ALL.class)
                    .put("deny all", Role.NONE.class)
                    .put("adminRoleCheck", User.AdminRoleCheck.class)
                    .put("initCheck", Child.InitCheck.class)
                    .put("initCheckOp", Child.InitCheckOp.class)
                    .put("FailCheckOp", Child.FailCheckOp.class)
                    .put("initCheckFilter", Child.InitCheckFilter.class)
                    .put("parentInitCheck", Parent.InitCheck.class)
                    .put("parentInitCheckOp", Parent.InitCheckOp.class)
                    .put("parentSpecialValue", Parent.SpecialValue.class)
                    .put("negativeIntegerUser", NegativeIntegerUserCheck.class)
                    .put("negativeChildId", NegativeChildIdCheck.class)
                    .put("noCommit", NoCommitEntity.NoCommitCheck.class)
                    .put("child4Parent10", Child4Parent10Check.class)
                    .put("checkActsLikeFilter", AnotherFilterExpressionCheckObj.CheckActsLikeFilter.class)
                    .put("noRead", CreateButNoRead.NOREAD.class)
                    .put("updateOnCreate", Common.UpdateOnCreate.class)
                    .put("checkLE", FilterExpressionCheckObj.CheckLE.class)
                    .put("checkRestrictUser", FilterExpressionCheckObj.CheckRestrictUser.class)
                    .put("specialValue", SpecialRead.SpecialValue.class)
                    .put("Field path editor check", Editor.FieldPathFilterExpression.class)
                    .build());
}
