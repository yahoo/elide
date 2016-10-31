/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.core.PersistentResourceTest;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.PermissionExecutorTest;
import com.yahoo.elide.security.checks.Check;
import com.yahoo.elide.security.checks.OperationCheck;
import com.yahoo.elide.security.checks.prefab.Role;

import java.util.HashMap;
import java.util.Optional;

/**
 * Check mappings to be used by tests.
 */
public class TestCheckMappings {
    public static final HashMap<String, Class<? extends Check>> MAPPINGS =
            new HashMap<String, Class<? extends Check>>() {
                {
                    put("allow all", Role.ALL.class);
                    put("deny all", Role.NONE.class);
                    put("changeSpecCollection", PersistentResourceTest.ChangeSpecCollection.class);
                    put("changeSpecNonCollection", PersistentResourceTest.ChangeSpecNonCollection.class);
                    put("initCheck", Child.InitCheck.class);
                    put("initCheckOp", Child.InitCheckOp.class);
                    put("parentInitCheck", Parent.InitCheck.class);
                    put("parentInitCheckOp", Parent.InitCheckOp.class);
                    put("negativeIntegerUser", NegativeIntegerUserCheck.class);
                    put("negativeChildId", NegativeChildIdCheck.class);
                    put("FailAtCommit", PermissionExecutorTest.FailingCommitCheck.class);
                    put("privatePermission", PrivatePermission.class);
                    put("sampleOperation", PermissionExecutorTest.SampleOperationCheck.class);
                    put("sampleCommit", PermissionExecutorTest.SampleCommitCheck.class);
                    put("sampleOperationCommitInverse", PermissionExecutorTest.SampleOperationCheckCommitInverse.class);
                    put("shouldCache", PermissionExecutorTest.ShouldCache.class);
                    put("peUserCheck", PermissionExecutorTest.UserCheckTest.class);
                    put("passingCommit", PermissionExecutorTest.PassingCommitCheck.class);
                }
            };

    private static final class PrivatePermission extends OperationCheck<Object> {
        @Override
        public boolean ok(Object object, com.yahoo.elide.security.RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            return false;
        }
    }
}
