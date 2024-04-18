/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.paiondata.elide.core.security.checks.Check;
import com.paiondata.elide.core.security.checks.prefab.Role;
import com.google.common.collect.ImmutableMap;

import java.util.HashMap;

/**
 * Check mappings to be used by tests.
 */
public class TestCheckMappings {
    public static final HashMap<String, Class<? extends Check>> MAPPINGS =
            new HashMap<>(ImmutableMap.<String, Class<? extends Check>>builder()
                    .put("Prefab.Role.All", Role.ALL.class)
                    .put("Prefab.Role.None", Role.NONE.class)
                    .put("adminRoleCheck", User.AdminRoleCheck.class)
                    .put("initCheck", Child.InitCheck.class)
                    .put("FailCheckOp", Child.FailCheckOp.class)
                    .put("initCheckFilter", Child.InitCheckFilter.class)
                    .put("parentInitCheck", Parent.InitCheck.class)
                    .put("parentSpecialValue", Parent.SpecialValue.class)
                    .put("negativeIntegerUser", NegativeIntegerUserCheck.class)
                    .put("negativeChildId", NegativeChildIdCheck.class)
                    .put("noCommit", NoCommitEntity.NoCommitCheck.class)
                    .put("child4Parent5", Child4Parent5Check.class)
                    .put("checkActsLikeFilter", AnotherFilterExpressionCheckObj.CheckActsLikeFilter.class)
                    .put("noRead", CreateButNoRead.NOREAD.class)
                    .put("checkLE", FilterExpressionCheckObj.CheckLE.class)
                    .put("checkRestrictUser", FilterExpressionCheckObj.CheckRestrictUser.class)
                    .put("specialValue", SpecialRead.SpecialValue.class)
                    .put("Field path editor check", Editor.FieldPathFilterExpression.class)
                    .build());
}
