/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.audit.AuditLogger;
import com.yahoo.elide.audit.TestAuditLogger;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.security.PermissionExecutor;
import com.yahoo.elide.security.User;
import com.yahoo.elide.security.executors.ActivePermissionExecutor;

import example.FunWithPermissions;
import example.TestCheckMappings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests audit functions inside RecordDao.
 */
public class PermissionAnnotationTest {
    private static final User GOOD_USER = new User(3);
    private static final User BAD_USER = new User(-1);

    private static PersistentResource<FunWithPermissions> funRecord;
    private static PersistentResource<FunWithPermissions> badRecord;

    private static EntityDictionary dictionary = new EntityDictionary(TestCheckMappings.MAPPINGS);

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

        RequestScope goodScope = new RequestScope(null, null, null, null, GOOD_USER, null, elideSettings);
        funRecord = new PersistentResource<>(fun, null, goodScope.getUUIDFor(fun), goodScope);
        RequestScope badScope = new RequestScope(null, null, null, null, BAD_USER, null, elideSettings);
        badRecord = new PersistentResource<>(fun, null, badScope.getUUIDFor(fun), badScope);
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
