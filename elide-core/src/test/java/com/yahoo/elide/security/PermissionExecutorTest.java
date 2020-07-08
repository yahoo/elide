/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security;

import static com.yahoo.elide.core.EntityDictionary.NO_VERSION;
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
import com.yahoo.elide.security.checks.OperationCheck;
import com.yahoo.elide.security.checks.UserCheck;
import com.yahoo.elide.security.permissions.ExpressionResult;

import example.TestCheckMappings;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
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

        PersistentResource resource = newResource(new Model(), Model.class, false);
        RequestScope requestScope = resource.getRequestScope();
        ChangeSpec cspec = new ChangeSpec(null, null, null, null);
        assertEquals(ExpressionResult.PASS,
                requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource, cspec));

        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testFailOperationCheckAll() throws Exception {
        @Entity
        @Include
        @UpdatePermission(expression = "sampleOperation AND deny all")
        class Model { }

        PersistentResource resource = newResource(new Model(), Model.class, false);
        RequestScope requestScope = resource.getRequestScope();
        assertThrows(
                ForbiddenAccessException.class,
                () -> requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource));
    }

    @Test
    public void testFailOperationCheckDeferred() throws Exception {
        @Entity
        @Include
        @UpdatePermission(expression = "sampleOperation")
        class Model { }

        PersistentResource resource = newResource(new Model(), Model.class, true);
        RequestScope requestScope = resource.getRequestScope();

        // Because the object is newly created, the check is DEFERRED.
        assertEquals(ExpressionResult.DEFERRED,
                requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource));

        assertThrows(ForbiddenAccessException.class, () -> requestScope.getPermissionExecutor().executeCommitChecks());
    }

    @Test
    public void testSuccessfulCommitChecks() throws Exception {
        @Entity
        @Include
        @UpdatePermission(expression = "sampleOperation")
        class Model { }

        PersistentResource resource = newResource(new Model(), Model.class, true);
        RequestScope requestScope = resource.getRequestScope();
        ChangeSpec cspec = new ChangeSpec(null, null, null, null);

        // Because the object is newly created, the check is DEFERRED.
        assertEquals(ExpressionResult.DEFERRED,
                requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource, cspec));

        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testReadFieldAwareSuccessAllAnyField() {
        PersistentResource resource = newResource(SampleBean.class, false);
        RequestScope requestScope = resource.getRequestScope();
        assertEquals(ExpressionResult.PASS,
                requestScope.getPermissionExecutor().checkPermission(ReadPermission.class, resource, new ChangeSpec(null, null, null, null)));
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testReadFieldAwareSuccessFailureAnyField() {
        PersistentResource resource = newResource(SampleBean.class, false);
        RequestScope requestScope = resource.getRequestScope();
        assertThrows(
                ForbiddenAccessException.class,
                () -> requestScope.getPermissionExecutor().checkPermission(ReadPermission.class, resource));
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testReadFieldAwareSuccessAll() {
        PersistentResource resource = newResource(SampleBean.class, false);
        RequestScope requestScope = resource.getRequestScope();
        assertEquals(ExpressionResult.PASS,
                requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, new ChangeSpec(null, null, null, null), ReadPermission.class, "allVisible"));
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testReadFieldAwareFailureAllSpecificField() {
        PersistentResource resource = newResource(SampleBean.class, false);
        RequestScope requestScope = resource.getRequestScope();
        assertThrows(
                ForbiddenAccessException.class,
                () -> requestScope.getPermissionExecutor().checkSpecificFieldPermissions(
                        resource, null, ReadPermission.class, "allVisible"));
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testReadFieldAwareFailureAllNoOverride() {
        PersistentResource resource = newResource(SampleBean.class, false);
        RequestScope requestScope = resource.getRequestScope();
        assertThrows(
                ForbiddenAccessException.class,
                () -> requestScope.getPermissionExecutor().checkSpecificFieldPermissions(
                        resource, null, ReadPermission.class, "defaultHidden"));
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testReadFieldAwareFailureAll() {
        PersistentResource resource = newResource(SampleBean.class, false);
        RequestScope requestScope = resource.getRequestScope();
        assertThrows(
                ForbiddenAccessException.class,
                () -> requestScope.getPermissionExecutor().checkSpecificFieldPermissions(
                        resource, null, ReadPermission.class, "cannotSeeMe"));
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testReadFieldAwareSuccessAny() {
        PersistentResource resource = newResource(SampleBean.class, false);
        RequestScope requestScope = resource.getRequestScope();
        assertEquals(ExpressionResult.PASS,
                requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, new ChangeSpec(null, null, null, null), ReadPermission.class, "mayFailInCommit"));
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testReadFieldAwareFailureAny() {
        PersistentResource resource = newResource(SampleBean.class, false);
        RequestScope requestScope = resource.getRequestScope();
        assertThrows(
                ForbiddenAccessException.class,
                () -> requestScope.getPermissionExecutor().checkSpecificFieldPermissions(
                        resource, null, ReadPermission.class, "mayFailInCommit"));
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testUpdateFieldAwareSuccessAll() {
        PersistentResource resource = newResource(SampleBean.class, true);
        RequestScope requestScope = resource.getRequestScope();
        assertEquals(ExpressionResult.DEFERRED,
                requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, new ChangeSpec(null, null, null, null), UpdatePermission.class, "allVisible"));
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testUpdateFieldAwareFailureAll() {
        PersistentResource resource = newResource(SampleBean.class, true);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, null, UpdatePermission.class, "allVisible");
        assertThrows(ForbiddenAccessException.class, () -> requestScope.getPermissionExecutor().executeCommitChecks());
    }

    @Test
    public void testUpdateFieldAwareSuccessAny() {
        PersistentResource resource = newResource(SampleBean.class, true);
        RequestScope requestScope = resource.getRequestScope();
        assertEquals(ExpressionResult.DEFERRED,
                requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, new ChangeSpec(null, null, null, null), UpdatePermission.class, "mayFailInCommit"));
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testUpdateFieldAwareFailureAny() {
        PersistentResource resource = newResource(SampleBean.class, true);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, null, UpdatePermission.class, "mayFailInCommit");
        assertThrows(ForbiddenAccessException.class, () -> requestScope.getPermissionExecutor().executeCommitChecks());
    }

    @Test
    public void testReadFieldAwareEntireOpenBean() {
        PersistentResource resource = newResource(OpenBean.class, false);
        RequestScope requestScope = resource.getRequestScope();
        assertEquals(ExpressionResult.PASS, requestScope.getPermissionExecutor().checkPermission(ReadPermission.class, resource));
        assertEquals(ExpressionResult.PASS, requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, null, ReadPermission.class, "open"));
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testReadFailureFieldAwareOpenBean() {
        PersistentResource resource = newResource(OpenBean.class, false);
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
        @UpdatePermission(expression = "deny all AND passingOp")
        class Model {
            @Id
            public Long id;

            @UpdatePermission(expression = "deny all OR passingOp")
            public String field = "some data";
        }

        PersistentResource resource = newResource(new Model(), Model.class, true);
        RequestScope requestScope = resource.getRequestScope();
        assertEquals(ExpressionResult.DEFERRED,
                requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource));
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

            @UpdatePermission(expression = "allow all AND FailOp")
            public String field = "some data";
        }

        PersistentResource resource = newResource(new Model(), Model.class, true);
        RequestScope requestScope = resource.getRequestScope();
        assertEquals(ExpressionResult.DEFERRED,
                requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource));
        assertThrows(ForbiddenAccessException.class, () -> requestScope.getPermissionExecutor().executeCommitChecks());
    }

    @Test
    public void testPassAnySpecificFieldAwareFailOperationSuccessCommit() {
        @Entity
        @Include
        @UpdatePermission(expression = "deny all AND passingOp")
        class Model {
            @Id
            public Long id;

            @UpdatePermission(expression = "deny all OR passingOp")
            public String field = "some data";
        }

        PersistentResource resource = newResource(new Model(), Model.class, true);
        RequestScope requestScope = resource.getRequestScope();
        assertEquals(ExpressionResult.DEFERRED,
                requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, null, UpdatePermission.class, "field"));
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

            @UpdatePermission(expression = "allow all AND FailOp")
            public String field = "some data";
        }

        PersistentResource resource = newResource(new Model(), Model.class, true);
        RequestScope requestScope = resource.getRequestScope();
        assertEquals(ExpressionResult.DEFERRED,
                requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, null, UpdatePermission.class, "field"));
        assertThrows(ForbiddenAccessException.class, () -> requestScope.getPermissionExecutor().executeCommitChecks());
    }

    @Test
    public void testBadInstance() {
        @Entity
        @Include
        @UpdatePermission(expression = "privatePermission")
        class Model { }

        PersistentResource resource = newResource(new Model(), Model.class, true);
        RequestScope requestScope = resource.getRequestScope();
        assertThrows(
                IllegalArgumentException.class,
                () -> requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource));
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testSpecificFieldOveriddenOperationCheckSucceed() {
        PersistentResource resource = newResource(CheckedEntity.class, true);
        RequestScope requestScope = resource.getRequestScope();
        // Should succeed in operation check despite the commit check failure
        assertEquals(ExpressionResult.DEFERRED,
                requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, null, UpdatePermission.class, "hello"));
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testSpecificFieldCommitCheckFailByOveriddenField() {
        PersistentResource resource = newResource(CheckedEntity.class, true);
        RequestScope requestScope = resource.getRequestScope();
        assertEquals(ExpressionResult.DEFERRED,
                requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, new ChangeSpec(null, null, null, null), UpdatePermission.class, "hello"));
        assertThrows(
                ForbiddenAccessException.class,
                () -> requestScope.getPermissionExecutor().executeCommitChecks());
    }

    @Test
    public void testReadCheckExpressionForNewlyCreatedObject() {
        @Entity
        @Include
        @ReadPermission(expression = "FailOp")
        class Model { }

        PersistentResource resource = newResource(new Model(), Model.class, true);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getDictionary().bindEntity(Model.class);
        assertEquals(ExpressionResult.DEFERRED, requestScope.getPermissionExecutor().checkPermission(ReadPermission.class, resource));
        assertThrows(ForbiddenAccessException.class, () -> requestScope.getPermissionExecutor().executeCommitChecks());
    }

    @Test
    public void testDeleteCheckExpressionForNewlyCreatedObject() {
        @Entity
        @Include
        @DeletePermission(expression = "FailOp")
        class Model { }

        PersistentResource resource = newResource(new Model(), Model.class, true);
        RequestScope requestScope = resource.getRequestScope();
        requestScope.getDictionary().bindEntity(Model.class);
        assertEquals(ExpressionResult.DEFERRED, requestScope.getPermissionExecutor().checkPermission(DeletePermission.class, resource));
        assertThrows(ForbiddenAccessException.class, () -> requestScope.getPermissionExecutor().executeCommitChecks());
    }

    @Test
    public void testCache() {
        PersistentResource resource = newResource(AnnotationOnlyRecord.class, false);
        RequestScope requestScope = resource.getRequestScope();
        assertEquals(ExpressionResult.PASS, requestScope.getPermissionExecutor().checkPermission(ReadPermission.class, resource));
        assertEquals(ExpressionResult.PASS, requestScope.getPermissionExecutor().checkPermission(ReadPermission.class, resource));
    }


    @Test
    public void testNoCache() {
        PersistentResource resource = newResource(AnnotationOnlyRecord.class, false);
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
        PersistentResource resource = newResource(UserCheckCacheRecord.class, false);
        RequestScope requestScope = resource.getRequestScope();
        ChangeSpec cspec = new ChangeSpec(null, null, null, null);
        // This should cache for updates, reads, etc.
        assertEquals(ExpressionResult.PASS, requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource, cspec));
        assertEquals(ExpressionResult.PASS, requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource, cspec));
        assertEquals(ExpressionResult.PASS, requestScope.getPermissionExecutor().checkPermission(ReadPermission.class, resource, cspec));
        assertEquals(ExpressionResult.PASS, requestScope.getPermissionExecutor().checkPermission(ReadPermission.class, resource, cspec));
    }

    @Test
    public void testUserCheckOnFieldSuccess() {
        PersistentResource resource = newResource(OpenBean.class, false);
        RequestScope requestScope = resource.getRequestScope();
        ExpressionResult result = requestScope.getPermissionExecutor().checkUserPermissions(OpenBean.class,
                ReadPermission.class,
                "open");

        assertEquals(ExpressionResult.PASS, result);
    }

    @Test
    public void testUserCheckOnFieldFailure() {
        PersistentResource resource = newResource(SampleBean.class, false);
        RequestScope requestScope = resource.getRequestScope();
        assertThrows(
                ForbiddenAccessException.class,
                () -> requestScope.getPermissionExecutor().checkUserPermissions(SampleBean.class,
                ReadPermission.class,
                "cannotSeeMe"));
    }

    @Test
    public void testUserCheckOnFieldDeferred() {
        PersistentResource resource = newResource(SampleBean.class, false);
        RequestScope requestScope = resource.getRequestScope();

        ExpressionResult result = requestScope.getPermissionExecutor().checkUserPermissions(SampleBean.class,
                ReadPermission.class,
                "allVisible");

        assertEquals(ExpressionResult.DEFERRED, result);
    }

    public <T> PersistentResource<T> newResource(T obj, Class<T> cls, boolean markNew) {
        EntityDictionary dictionary = new EntityDictionary(TestCheckMappings.MAPPINGS);
        dictionary.bindEntity(cls);
        RequestScope requestScope = new RequestScope(null, NO_VERSION, null, null, null, null, UUID.randomUUID(), getElideSettings(dictionary));
        PersistentResource resource = new PersistentResource<>(obj, null, requestScope.getUUIDFor(obj), requestScope);
        if (markNew) {
            requestScope.getNewPersistentResources().add(resource);
        }
        return resource;
    }

    public <T> PersistentResource<T> newResource(Class<T> cls, boolean markNew) {
        try {
            T obj = cls.newInstance();
            return newResource(obj, cls, markNew);
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


    public static final class SampleOperationCheckInverse extends OperationCheck<Object> {
        @Override
        public boolean ok(Object object, com.yahoo.elide.security.RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            return !changeSpec.isPresent();
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
        @UpdatePermission(expression = "allow all AND sampleOperation")
        public String allVisible = "You should see me!";

        public String defaultHidden = "I'm invisible. muwahaha...";

        @ReadPermission(expression = "allow all AND deny all")
        @UpdatePermission(expression = "allow all AND deny all")
        public String cannotSeeMe = "hidden";

        @ReadPermission(expression = "sampleOperation")
        @UpdatePermission(expression =  "sampleOperation OR deny all")
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

        @ReadPermission(expression = "allow all AND sampleOperation")
        @UpdatePermission(expression = "allow all AND sampleOperation")
        public String openAll = "all";

        @ReadPermission(expression = "deny all OR sampleOperation")
        @UpdatePermission(expression = "deny all OR sampleOperation")
        public String openAny = "all";
    }

    @Entity
    @Include
    @UpdatePermission(expression = "sampleOperation")
    public static final class CheckedEntity {
        @Id
        public Long id;

        @UpdatePermission(expression = "sampleOperationInverse")
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

    public static final class PassingOperationCheck extends OperationCheck<Object> {
        @Override
        public boolean ok(Object object, com.yahoo.elide.security.RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            return true;
        }
    }

    public static final class FailingOperationCheck extends OperationCheck<Object> {
        @Override
        public boolean ok(Object object, com.yahoo.elide.security.RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            return false;
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
