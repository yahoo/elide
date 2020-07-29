/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import com.yahoo.elide.security.permissions.ExpressionResult;

import example.TestCheckMappings;

import org.junit.jupiter.api.Test;

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

    @Test
    public void testFailOperationCheckAll() throws Exception {
        @Entity
        @Include
        @UpdatePermission(expression = "sampleOperation AND deny all")
        class Model { }

        PersistentResource resource = newResource(new Model(), Model.class);
        RequestScope requestScope = resource.getRequestScope();
        assertThrows(
                ForbiddenAccessException.class,
                () -> requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource));
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testFailOperationCheckAny() throws Exception {
        @Entity
        @Include
        @UpdatePermission(expression = "sampleCommit OR sampleOperation")
        class Model { }

        PersistentResource resource = newResource(new Model(), Model.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource);
        // Update permissions are deferred. In the case of "any," commit checks must execute before failure can be detected
        assertThrows(ForbiddenAccessException.class, () -> requestScope.getPermissionExecutor().executeCommitChecks());
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


    @Test
    public void testFailCommitChecks() throws Exception {
        @Entity
        @Include
        @UpdatePermission(expression = "sampleCommit")
        class Model { }

        PersistentResource resource = newResource(new Model(), Model.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource);
        assertThrows(ForbiddenAccessException.class, () -> requestScope.getPermissionExecutor().executeCommitChecks());
    }

    @Test
    public void testReadFieldAwareSuccessAllAnyField() {
        PersistentResource resource = newResource(SampleBean.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionExecutor().checkPermission(ReadPermission.class, resource, new ChangeSpec(null, null, null, null));
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testReadFieldAwareSuccessFailureAnyField() {
        PersistentResource resource = newResource(SampleBean.class);
        RequestScope requestScope = resource.getRequestScope();
        assertThrows(
                ForbiddenAccessException.class,
                () -> requestScope.getPermissionExecutor().checkPermission(ReadPermission.class, resource));
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testReadFieldAwareSuccessAll() {
        PersistentResource resource = newResource(SampleBean.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, new ChangeSpec(null, null, null, null), ReadPermission.class, "allVisible");
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testReadFieldAwareFailureAllSpecificField() {
        PersistentResource resource = newResource(SampleBean.class);
        RequestScope requestScope = resource.getRequestScope();
        assertThrows(
                ForbiddenAccessException.class,
                () -> requestScope.getPermissionExecutor().checkSpecificFieldPermissions(
                        resource, null, ReadPermission.class, "allVisible"));
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testReadFieldAwareFailureAllNoOverride() {
        PersistentResource resource = newResource(SampleBean.class);
        RequestScope requestScope = resource.getRequestScope();
        assertThrows(
                ForbiddenAccessException.class,
                () -> requestScope.getPermissionExecutor().checkSpecificFieldPermissions(
                        resource, null, ReadPermission.class, "defaultHidden"));
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testReadFieldAwareFailureAll() {
        PersistentResource resource = newResource(SampleBean.class);
        RequestScope requestScope = resource.getRequestScope();
        assertThrows(
                ForbiddenAccessException.class,
                () -> requestScope.getPermissionExecutor().checkSpecificFieldPermissions(
                        resource, null, ReadPermission.class, "cannotSeeMe"));
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testReadFieldAwareSuccessAny() {
        PersistentResource resource = newResource(SampleBean.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, new ChangeSpec(null, null, null, null), ReadPermission.class, "mayFailInCommit");
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testReadFieldAwareFailureAny() {
        PersistentResource resource = newResource(SampleBean.class);
        RequestScope requestScope = resource.getRequestScope();
        assertThrows(
                ForbiddenAccessException.class,
                () -> requestScope.getPermissionExecutor().checkSpecificFieldPermissions(
                        resource, null, ReadPermission.class, "mayFailInCommit"));
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testUpdateFieldAwareSuccessAll() {
        PersistentResource resource = newResource(SampleBean.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, new ChangeSpec(null, null, null, null), UpdatePermission.class, "allVisible");
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testUpdateFieldAwareFailureAll() {
        PersistentResource resource = newResource(SampleBean.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, null, UpdatePermission.class, "allVisible");
        assertThrows(ForbiddenAccessException.class, () -> requestScope.getPermissionExecutor().executeCommitChecks());
    }

    @Test
    public void testUpdateFieldAwareSuccessAny() {
        PersistentResource resource = newResource(SampleBean.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, new ChangeSpec(null, null, null, null), UpdatePermission.class, "mayFailInCommit");
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testUpdateFieldAwareFailureAny() {
        PersistentResource resource = newResource(SampleBean.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, null, UpdatePermission.class, "mayFailInCommit");
        assertThrows(ForbiddenAccessException.class, () -> requestScope.getPermissionExecutor().executeCommitChecks());
    }

    @Test
    public void testReadFieldAwareEntireOpenBean() {
        PersistentResource resource = newResource(OpenBean.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionExecutor().checkPermission(ReadPermission.class, resource);
        requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, null, ReadPermission.class, "open");
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testReadFailureFieldAwareOpenBean() {
        PersistentResource resource = newResource(OpenBean.class);
        RequestScope requestScope = resource.getRequestScope();
        assertThrows(
                ForbiddenAccessException.class,
                () -> requestScope.getPermissionExecutor().checkSpecificFieldPermissions(
                        resource, null, ReadPermission.class, "openAll"));
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

    @Test
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
        assertThrows(ForbiddenAccessException.class, () -> requestScope.getPermissionExecutor().executeCommitChecks());
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

    @Test
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
        assertThrows(ForbiddenAccessException.class, () -> requestScope.getPermissionExecutor().executeCommitChecks());
    }

    @Test
    public void testBadInstance() {
        @Entity
        @Include
        @UpdatePermission(expression = "privatePermission")
        class Model { }

        PersistentResource resource = newResource(new Model(), Model.class);
        RequestScope requestScope = resource.getRequestScope();
        assertThrows(
                IllegalArgumentException.class,
                () -> requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource));
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

    @Test
    public void testSpecificFieldCommitCheckFailByOveriddenField() {
        PersistentResource resource = newResource(CommitCheckEntity.class);
        RequestScope requestScope = resource.getRequestScope();
        // Should succeed in commit check despite the operation check failure
        assertThrows(
                ForbiddenAccessException.class,
                () -> requestScope.getPermissionExecutor().checkSpecificFieldPermissions(
                        resource, new ChangeSpec(null, null, null, null), UpdatePermission.class, "hello"));
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testReadCheckExpressionAlwaysInline() {
        @Entity
        @Include
        @ReadPermission(expression = "FailAtCommit")
        class Model { }

        PersistentResource resource = newResource(new Model(), Model.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getDictionary().bindEntity(Model.class);
        assertThrows(
                ForbiddenAccessException.class,
                () -> requestScope.getPermissionExecutor().checkPermission(ReadPermission.class, resource));
        // NOTE: This check should throw a ForbiddenAccess since commit-time checks should be converted
        //       to inline checks. As a result, DO NOT call executeCommitChecks() in this test.
    }

    @Test
    public void testDeleteCheckExpressionAlwaysInline() {
        @Entity
        @Include
        @DeletePermission(expression = "FailAtCommit")
        class Model { }

        PersistentResource resource = newResource(new Model(), Model.class);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getDictionary().bindEntity(Model.class);
        assertThrows(
                ForbiddenAccessException.class,
                () -> requestScope.getPermissionExecutor().checkPermission(DeletePermission.class, resource));
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

    @Test
    public void testNoCache() {
        PersistentResource resource = newResource(AnnotationOnlyRecord.class);
        RequestScope requestScope = resource.getRequestScope();
        ChangeSpec cspec = new ChangeSpec(null, null, null, null);
        assertThrows(
                ForbiddenAccessException.class,
                () -> requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource, cspec));
        assertThrows(
                ForbiddenAccessException.class,
                () -> requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource, cspec));
    }

    @Test
    public void testUserCheckCache() {
        PersistentResource<UserCheckCacheRecord> resource = newResource(UserCheckCacheRecord.class);
        RequestScope requestScope = resource.getRequestScope();
        ChangeSpec cspec = new ChangeSpec(null, null, null, null);
        // This should cache for updates, reads, etc.
        requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource, cspec);
        requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource, cspec);
        requestScope.getPermissionExecutor().checkPermission(ReadPermission.class, resource, cspec);
        requestScope.getPermissionExecutor().checkPermission(ReadPermission.class, resource, cspec);
    }

    @Test
    public void testUserCheckOnFieldSuccess() {
        PersistentResource resource = newResource(OpenBean.class);
        RequestScope requestScope = resource.getRequestScope();
        ExpressionResult result = requestScope.getPermissionExecutor().checkUserPermissions(OpenBean.class,
                ReadPermission.class,
                "open");

        assertEquals(ExpressionResult.PASS, result);
    }

    @Test
    public void testUserCheckOnFieldFailure() {
        PersistentResource resource = newResource(SampleBean.class);
        RequestScope requestScope = resource.getRequestScope();
        assertThrows(
                ForbiddenAccessException.class,
                () -> requestScope.getPermissionExecutor().checkUserPermissions(SampleBean.class,
                ReadPermission.class,
                "cannotSeeMe"));
    }

    @Test
    public void testUserCheckOnFieldDeferred() {
        PersistentResource resource = newResource(SampleBean.class);
        RequestScope requestScope = resource.getRequestScope();

        ExpressionResult result = requestScope.getPermissionExecutor().checkUserPermissions(SampleBean.class,
                ReadPermission.class,
                "allVisible");

        assertEquals(ExpressionResult.DEFERRED, result);
    }

    public <T> PersistentResource<T> newResource(T obj, Class<T> cls) {
        EntityDictionary dictionary = new EntityDictionary(TestCheckMappings.MAPPINGS);
        dictionary.bindEntity(cls);
        RequestScope requestScope = new RequestScope(null, null, null, null, null, null, getElideSettings(dictionary));
        return new PersistentResource<>(obj, null, requestScope.getUUIDFor(obj), requestScope);
    }

    public <T> PersistentResource<T> newResource(Class<T> cls) {
        EntityDictionary dictionary = new EntityDictionary(TestCheckMappings.MAPPINGS);
        dictionary.bindEntity(cls);
        RequestScope requestScope = new RequestScope(null, null, null, null, null, null, getElideSettings(dictionary));
        try {
            T obj = cls.newInstance();
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
