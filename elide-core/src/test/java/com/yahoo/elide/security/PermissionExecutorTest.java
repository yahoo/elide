/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.security.checks.CommitCheck;
import com.yahoo.elide.security.checks.OperationCheck;
import com.yahoo.elide.security.checks.UserCheck;

import example.TestCheckMappings;

import org.testng.annotations.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.persistence.Entity;
import javax.persistence.Id;

public class PermissionExecutorTest {

    @Test
    public void testSuccessfulOperationCheck() throws Exception {
        @Entity
        @Include
        @UpdatePermission(expression = "sampleOperation")
        class Model { }

        PersistentResource resource = newResource(new Model(), Model.class);
        RequestScope requestScope = resource.getRequestScope();
        ChangeSpec cspec = new ChangeSpec(null, null, null, null);
        requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource, cspec);
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testFailOperationCheckAll() throws Exception {
        @Entity
        @Include
        @UpdatePermission(expression = "sampleOperation AND deny all")
        class Model { }

        PersistentResource resource = newResource(new Model(), Model.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource);
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testFailOperationCheckAny() throws Exception {
        @Entity
        @Include
        @UpdatePermission(expression = "sampleCommit OR sampleOperation")
        class Model { }

        PersistentResource resource = newResource(new Model(), Model.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource);
        // Update permissions are deferred. In the case of "any," commit checks must execute before failure can be detected
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testSuccessfulCommitChecks() throws Exception {
        @Entity
        @Include
        @UpdatePermission(expression = "sampleCommit")
        class Model { }

        PersistentResource resource = newResource(new Model(), Model.class);
        RequestScope requestScope = resource.getRequestScope();
        ChangeSpec cspec = new ChangeSpec(null, null, null, null);
        requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource, cspec);
        requestScope.getPermissionExecutor().executeCommitChecks();
    }


    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testFailCommitChecks() throws Exception {
        @Entity
        @Include
        @UpdatePermission(expression = "sampleCommit")
        class Model { }

        PersistentResource resource = newResource(new Model(), Model.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource);
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testReadFieldAwareSuccessAllAnyField() {
        PersistentResource resource = newResource(SampleBean.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionExecutor().checkPermission(ReadPermission.class, resource, new ChangeSpec(null, null, null, null));
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testReadFieldAwareSuccessFailureAnyField() {
        PersistentResource resource = newResource(SampleBean.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionExecutor().checkPermission(ReadPermission.class, resource);
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testReadFieldAwareSuccessAll() {
        PersistentResource resource = newResource(SampleBean.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, new ChangeSpec(null, null, null, null), ReadPermission.class, "allVisible");
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testReadFieldAwareFailureAllSpecificField() {
        PersistentResource resource = newResource(SampleBean.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, null, ReadPermission.class, "allVisible");
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testReadFieldAwareFailureAllNoOverride() {
        PersistentResource resource = newResource(SampleBean.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, null, ReadPermission.class, "defaultHidden");
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testReadFieldAwareFailureAll() {
        PersistentResource resource = newResource(SampleBean.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, null, ReadPermission.class, "cannotSeeMe");
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testReadFieldAwareSuccessAny() {
        PersistentResource resource = newResource(SampleBean.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, new ChangeSpec(null, null, null, null), ReadPermission.class, "mayFailInCommit");
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testReadFieldAwareFailureAny() {
        PersistentResource resource = newResource(SampleBean.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, null, ReadPermission.class, "mayFailInCommit");
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testUpdateFieldAwareSuccessAll() {
        PersistentResource resource = newResource(SampleBean.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, new ChangeSpec(null, null, null, null), UpdatePermission.class, "allVisible");
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testUpdateFieldAwareFailureAll() {
        PersistentResource resource = newResource(SampleBean.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, null, UpdatePermission.class, "allVisible");
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testUpdateFieldAwareSuccessAny() {
        PersistentResource resource = newResource(SampleBean.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, new ChangeSpec(null, null, null, null), UpdatePermission.class, "mayFailInCommit");
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testUpdateFieldAwareFailureAny() {
        PersistentResource resource = newResource(SampleBean.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, null, UpdatePermission.class, "mayFailInCommit");
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testReadFieldAwareEntireOpenBean() {
        PersistentResource resource = newResource(OpenBean.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionExecutor().checkPermission(ReadPermission.class, resource);
        requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, null, ReadPermission.class, "open");
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testReadFailureFieldAwareOpenBean() {
        PersistentResource resource = newResource(OpenBean.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, null, ReadPermission.class, "openAll");
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testPassAnyFieldAwareFailOperationSuccessCommit() {
        @Entity
        @Include
        @UpdatePermission(expression = "deny all AND passingCommit")
        class Model {
            @Id
            public Long id;

            @UpdatePermission(expression = "deny all OR passingCommit")
            public String field = "some data";
        }

        PersistentResource resource = newResource(new Model(), Model.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource);
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test(expectedExceptions = {ForbiddenAccessException.class})
    public void testFailAllFieldAwareSuccessOperationFailCommit() {
        @Entity
        @Include
        @UpdatePermission(expression = "deny all")
        class Model {
            @Id
            public Long id;

            @UpdatePermission(expression = "allow all AND FailAtCommit")
            public String field = "some data";
        }

        PersistentResource resource = newResource(new Model(), Model.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource);
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testPassAnySpecificFieldAwareFailOperationSuccessCommit() {
        @Entity
        @Include
        @UpdatePermission(expression = "deny all AND passingCommit")
        class Model {
            @Id
            public Long id;

            @UpdatePermission(expression = "deny all OR passingCommit")
            public String field = "some data";
        }

        PersistentResource resource = newResource(new Model(), Model.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, null, UpdatePermission.class, "field");
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test(expectedExceptions = {ForbiddenAccessException.class})
    public void testFailAllSpecificFieldAwareSuccessOperationFailCommit() {
        @Entity
        @Include
        @UpdatePermission(expression = "allow all")
        class Model {
            @Id
            public Long id;

            @UpdatePermission(expression = "allow all AND FailAtCommit")
            public String field = "some data";
        }

        PersistentResource resource = newResource(new Model(), Model.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, null, UpdatePermission.class, "field");
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test(expectedExceptions = {IllegalArgumentException.class})
    public void testBadInstance() {
        @Entity
        @Include
        @UpdatePermission(expression = "privatePermission")
        class Model { }

        PersistentResource resource = newResource(new Model(), Model.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource);
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testSpecificFieldOveriddenOperationCheckSucceed() {
        PersistentResource resource = newResource(CommitCheckEntity.class);
        RequestScope requestScope = resource.getRequestScope();
        // Should succeed in operation check despite the commit check failure
        requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, null, UpdatePermission.class, "hello");
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test(expectedExceptions = {ForbiddenAccessException.class})
    public void testSpecificFieldCommitCheckFailByOveriddenField() {
        PersistentResource resource = newResource(CommitCheckEntity.class);
        RequestScope requestScope = resource.getRequestScope();
        // Should succeed in commit check despite the operation check failure
        requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, new ChangeSpec(null, null, null, null), UpdatePermission.class, "hello");
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testReadCheckExpressionAlwaysInline() {
        @Entity
        @Include
        @ReadPermission(expression = "FailAtCommit")
        class Model { }

        PersistentResource resource = newResource(new Model(), Model.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getDictionary().bindEntity(Model.class);
        requestScope.getPermissionExecutor().checkPermission(ReadPermission.class, resource);
        // NOTE: This check should throw a ForbiddenAccess since commit-time checks should be converted
        //       to inline checks. As a result, DO NOT call executeCommitChecks() in this test.
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testDeleteCheckExpressionAlwaysInline() {
        @Entity
        @Include
        @DeletePermission(expression = "FailAtCommit")
        class Model { }

        PersistentResource resource = newResource(new Model(), Model.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getDictionary().bindEntity(Model.class);
        requestScope.getPermissionExecutor().checkPermission(DeletePermission.class, resource);
        // NOTE: This check should throw a ForbiddenAccess since commit-time checks should be converted
        //       to inline checks. As a result, DO NOT call executeCommitChecks() in this test.
    }

    @Test
    public void testCache() {
        PersistentResource resource = newResource(AnnotationOnlyRecord.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionExecutor().checkPermission(ReadPermission.class, resource);
        requestScope.getPermissionExecutor().checkPermission(ReadPermission.class, resource);
    }

    @Test(expectedExceptions = {ForbiddenAccessException.class})
    public void testNoCache() {
        PersistentResource resource = newResource(AnnotationOnlyRecord.class);
        RequestScope requestScope = resource.getRequestScope();
        ChangeSpec cspec = new ChangeSpec(null, null, null, null);
        requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource, cspec);
        requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource, cspec);
    }

    @Test
    public void testUserCheckCache() {
        PersistentResource resource = newResource(UserCheckCacheRecord.class);
        RequestScope requestScope = resource.getRequestScope();
        ChangeSpec cspec = new ChangeSpec(null, null, null, null);
        // This should cache for updates, reads, etc.
        requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource, cspec);
        requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource, cspec);
        requestScope.getPermissionExecutor().checkPermission(ReadPermission.class, resource, cspec);
        requestScope.getPermissionExecutor().checkPermission(ReadPermission.class, resource, cspec);
    }

    public <T> PersistentResource newResource(T obj, Class<T> cls) {
        EntityDictionary dictionary = new EntityDictionary(TestCheckMappings.MAPPINGS);
        dictionary.bindEntity(cls);
        RequestScope requestScope = new RequestScope(null, null, null, null, null, getElideSettings(dictionary), false);
        return new PersistentResource<>(obj, null, requestScope.getUUIDFor(obj), requestScope);
    }

    public PersistentResource newResource(Class cls) {
        EntityDictionary dictionary = new EntityDictionary(TestCheckMappings.MAPPINGS);
        dictionary.bindEntity(cls);
        RequestScope requestScope = new RequestScope(null, null, null, null, null, getElideSettings(dictionary), false);
        try {
            Object obj = cls.newInstance();
            return new PersistentResource<>(obj, null, requestScope.getUUIDFor(obj), requestScope);
        } catch (InstantiationException | IllegalAccessException e) {
            return null;
        }
    }

    private ElideSettings getElideSettings(EntityDictionary dictionary) {
        return new ElideSettingsBuilder(null)
                    .withEntityDictionary(dictionary)
                    .build();
    }

    public static final class SampleOperationCheck extends OperationCheck<Object> {
        @Override
        public boolean ok(Object object, com.yahoo.elide.security.RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            return changeSpec.isPresent();
        }
    }

    public static final class SampleCommitCheck extends CommitCheck<Object> {
        @Override
        public boolean ok(Object object, com.yahoo.elide.security.RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            return changeSpec.isPresent();
        }
    }

    public static final class SampleOperationCheckCommitInverse extends OperationCheck<Object> {
        @Override
        public boolean ok(Object object, com.yahoo.elide.security.RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            return !changeSpec.isPresent();
        }
    }

    public static final class PassingCommitCheck extends CommitCheck<Object> {
        @Override
        public boolean ok(Object object, com.yahoo.elide.security.RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            return true;
        }
    }

    public static final class FailingCommitCheck extends CommitCheck<Object> {
        @Override
        public boolean ok(Object object, com.yahoo.elide.security.RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            return false;
        }

        @Override
        public String checkIdentifier() {
            return "FailAtCommit";
        }
    }

    @ReadPermission(expression = "deny all")
    @UpdatePermission(expression = "deny all")
    @Include
    @Entity
    public static final class SampleBean {
        @Id
        public Long id;

        @ReadPermission(expression = "allow all AND sampleOperation")
        @UpdatePermission(expression = "allow all AND sampleCommit")
        public String allVisible = "You should see me!";

        public String defaultHidden = "I'm invisible. muwahaha...";

        @ReadPermission(expression = "allow all AND deny all")
        @UpdatePermission(expression = "allow all AND deny all")
        public String cannotSeeMe = "hidden";

        @ReadPermission(expression = "sampleOperation")
        @UpdatePermission(expression =  "sampleCommit OR deny all")
        public String mayFailInCommit = "aw :(";
    }

    @ReadPermission(expression = "allow all")
    @UpdatePermission(expression = "allow all")
    @Include
    @Entity
    public static final class OpenBean {
        @Id
        public Long id;

        public String open;

        @ReadPermission(expression = "deny all AND sampleOperation")
        @UpdatePermission(expression = "sampleCommit AND sampleOperation")
        public String openAll = "all";

        @ReadPermission(expression = "sampleOperation OR sampleOperation")
        @UpdatePermission(expression = "sampleCommit OR sampleOperation")
        public String openAny = "all";
    }

    @Entity
    @Include
    @UpdatePermission(expression = "sampleCommit")
    public static final class CommitCheckEntity {
        @Id
        public Long id;

        @UpdatePermission(expression = "sampleOperationCommitInverse")
        public String hello;
    }

    /* Cache testing */

    public static class ShouldCache extends OperationCheck<Object> {
        private static AtomicBoolean hasRun = new AtomicBoolean(false);
        @Override
        public boolean ok(Object object, com.yahoo.elide.security.RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            return !hasRun.getAndSet(true);
        }
    }

    @Entity
    @Include
    @ReadPermission(expression = "shouldCache")
    @UpdatePermission(expression = "shouldCache")
    public static class AnnotationOnlyRecord {
    }

    /* UserCheck cache testing */

    public static class UserCheckTest extends UserCheck {
        public AtomicBoolean hasBeenCalled = new AtomicBoolean(false);
        @Override
        public boolean ok(User user) {
            return !hasBeenCalled.getAndSet(true);
        }
    }

    @Entity
    @Include
    @ReadPermission(expression = "peUserCheck")
    @UpdatePermission(expression = "peUserCheck")
    public static class UserCheckCacheRecord {
    }
}
