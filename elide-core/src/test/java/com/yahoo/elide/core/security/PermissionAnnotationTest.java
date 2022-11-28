/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.security;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.audit.AuditLogger;
import com.yahoo.elide.core.audit.TestAuditLogger;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.dictionary.TestDictionary;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.core.security.executors.ActivePermissionExecutor;
import example.FunWithPermissions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

/**
 * Tests audit functions inside RecordDao.
 */
public class PermissionAnnotationTest {
    private static final User GOOD_USER = new TestUser("3");
    private static final User BAD_USER = new TestUser("-1");

    private static com.yahoo.elide.core.PersistentResource<FunWithPermissions> funRecord;
    private static com.yahoo.elide.core.PersistentResource<FunWithPermissions> badRecord;

    private static EntityDictionary dictionary = TestDictionary.getTestDictionary();

    public PermissionAnnotationTest() { }

    @BeforeAll
    public static void setup() {
        dictionary.bindEntity(FunWithPermissions.class);

        FunWithPermissions fun = new FunWithPermissions();
        fun.setId(1);

        AuditLogger testLogger = new TestAuditLogger();

        ElideSettings elideSettings = new ElideSettingsBuilder(null)
                .withDefaultPageSize(10)
                .withDefaultMaxPageSize(10)
                .withAuditLogger(testLogger)
                .withEntityDictionary(dictionary)
                .build();

        RequestScope goodScope = new RequestScope(null, null, NO_VERSION, null, null, GOOD_USER, null, null, UUID.randomUUID(), elideSettings);
        funRecord = new PersistentResource<>(fun, goodScope.getUUIDFor(fun), goodScope);
        RequestScope badScope = new RequestScope(null, null, NO_VERSION, null, null, BAD_USER, null, null, UUID.randomUUID(), elideSettings);
        badRecord = new PersistentResource<>(fun, badScope.getUUIDFor(fun), badScope);
    }

    @Test
    public void testClassAnyOk() {
        final PermissionExecutor permissionExecutor = new ActivePermissionExecutor(funRecord.getRequestScope());
        permissionExecutor.checkPermission(ReadPermission.class, funRecord);
        permissionExecutor.checkPermission(UpdatePermission.class, funRecord);
        permissionExecutor.checkPermission(CreatePermission.class, funRecord);
    }

    @Test
    public void testClassAllNotOk() {
        final PermissionExecutor permissionExecutor = new ActivePermissionExecutor(badRecord.getRequestScope());
        assertThrows(
                ForbiddenAccessException.class,
                () -> permissionExecutor.checkPermission(DeletePermission.class, funRecord));
    }

    @Test
    public void testFieldPermissionOk() {
        final PermissionExecutor permissionExecutor = new ActivePermissionExecutor(funRecord.getRequestScope());
        permissionExecutor.checkSpecificFieldPermissions(funRecord, null, ReadPermission.class, "field3");
        permissionExecutor.checkSpecificFieldPermissions(funRecord, null, ReadPermission.class, "relation1");
        permissionExecutor.checkSpecificFieldPermissions(funRecord, null, ReadPermission.class, "relation2");
    }

    @Test
    public void testField3PermissionNotOk() {
        final PermissionExecutor permissionExecutor = new ActivePermissionExecutor(badRecord.getRequestScope());
        assertThrows(
                ForbiddenAccessException.class,
                () -> permissionExecutor.checkSpecificFieldPermissions(
                        badRecord, null, ReadPermission.class, "field3"));
    }

    @Test
    public void testRelation1PermissionNotOk() {
        final PermissionExecutor permissionExecutor = new ActivePermissionExecutor(badRecord.getRequestScope());
        assertThrows(
                ForbiddenAccessException.class,
                () -> permissionExecutor.checkSpecificFieldPermissions(
                        badRecord, null, ReadPermission.class, "relation1"));
    }

    @Test
    public void testRelation2PermissionNotOk() {
        final PermissionExecutor permissionExecutor = new ActivePermissionExecutor(badRecord.getRequestScope());
        assertThrows(
                ForbiddenAccessException.class,
                () -> permissionExecutor.checkSpecificFieldPermissions(
                        badRecord, null, ReadPermission.class, "relation2"));
    }

    /**
     * Verifies ANY where the first fails but the last succeeds.
     */
    @Test()
    public void testField5PermissionOk() {
        final PermissionExecutor permissionExecutor = new ActivePermissionExecutor(badRecord.getRequestScope());
        permissionExecutor.checkSpecificFieldPermissions(badRecord, null, ReadPermission.class, "field5");
    }

    /**
     * Verifies ALL where the first fails.
     */
    @Test
    public void testField6PermissionNotOk() {
        final PermissionExecutor permissionExecutor = new ActivePermissionExecutor(badRecord.getRequestScope());
        assertThrows(
                ForbiddenAccessException.class,
                () -> permissionExecutor.checkSpecificFieldPermissions(
                        badRecord, null, ReadPermission.class, "field6"));
    }

    /**
     * Verifies ALL where the last fails.
     */
    @Test
    public void testField7PermissionNotOk() {
        final PermissionExecutor permissionExecutor = new ActivePermissionExecutor(badRecord.getRequestScope());
        assertThrows(
                ForbiddenAccessException.class,
                () -> permissionExecutor.checkSpecificFieldPermissions(
                        badRecord, null, ReadPermission.class, "field7"));
    }

    /**
     * Verifies ANY where all fail.
     */
    @Test
    public void testField8PermissionNotOk() {
        final PermissionExecutor permissionExecutor = new ActivePermissionExecutor(badRecord.getRequestScope());
        assertThrows(
                ForbiddenAccessException.class,
                () -> permissionExecutor.checkSpecificFieldPermissions(
                        badRecord, null, ReadPermission.class, "field8"));
    }
}
