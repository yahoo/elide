/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

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

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * Tests audit functions inside RecordDao.
 */
public class PermissionAnnotationTest {
    private PersistentResource<FunWithPermissions> funRecord;
    private final User goodUser;
    private PersistentResource<FunWithPermissions> badRecord;
    private final User badUser;
    private final EntityDictionary dictionary;

    public PermissionAnnotationTest() {
        goodUser = new User(3);
        badUser = new User(-1);
        dictionary = new EntityDictionary(TestCheckMappings.MAPPINGS);
    }

    @BeforeTest
    public void setup() {
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

        RequestScope goodScope = new RequestScope(null, null, null, goodUser, null, elideSettings, false);
        funRecord = new PersistentResource<>(fun, null, goodScope.getUUIDFor(fun), goodScope);
        RequestScope badScope = new RequestScope(null, null, null, badUser, null, elideSettings, false);
        badRecord = new PersistentResource<>(fun, null, badScope.getUUIDFor(fun), badScope);
    }

    @Test
    public void testClassAnyOk() {
        final PermissionExecutor permissionExecutor = new ActivePermissionExecutor(funRecord.getRequestScope());
        permissionExecutor.checkPermission(ReadPermission.class, funRecord);
        permissionExecutor.checkPermission(UpdatePermission.class, funRecord);
        permissionExecutor.checkPermission(CreatePermission.class, funRecord);
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testClassAllNotOk() {
        final PermissionExecutor permissionExecutor = new ActivePermissionExecutor(badRecord.getRequestScope());
        permissionExecutor.checkPermission(DeletePermission.class, funRecord);
    }

    @Test
    public void testFieldPermissionOk() {
        final PermissionExecutor permissionExecutor = new ActivePermissionExecutor(funRecord.getRequestScope());
        permissionExecutor.checkSpecificFieldPermissions(funRecord, null, ReadPermission.class, "field3");
        permissionExecutor.checkSpecificFieldPermissions(funRecord, null, ReadPermission.class, "relation1");
        permissionExecutor.checkSpecificFieldPermissions(funRecord, null, ReadPermission.class, "relation2");
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testField3PermissionNotOk() {
        final PermissionExecutor permissionExecutor = new ActivePermissionExecutor(badRecord.getRequestScope());
        permissionExecutor.checkSpecificFieldPermissions(badRecord, null, ReadPermission.class, "field3");
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testRelation1PermissionNotOk() {
        final PermissionExecutor permissionExecutor = new ActivePermissionExecutor(badRecord.getRequestScope());
        permissionExecutor.checkSpecificFieldPermissions(badRecord, null, ReadPermission.class, "relation1");
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testRelation2PermissionNotOk() {
        final PermissionExecutor permissionExecutor = new ActivePermissionExecutor(badRecord.getRequestScope());
        permissionExecutor.checkSpecificFieldPermissions(badRecord, null, ReadPermission.class, "relation2");
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
    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testField6PermissionNotOk() {
        final PermissionExecutor permissionExecutor = new ActivePermissionExecutor(badRecord.getRequestScope());
        permissionExecutor.checkSpecificFieldPermissions(badRecord, null, ReadPermission.class, "field6");
    }

    /**
     * Verifies ALL where the last fails.
     */
    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testField7PermissionNotOk() {
        final PermissionExecutor permissionExecutor = new ActivePermissionExecutor(badRecord.getRequestScope());
        permissionExecutor.checkSpecificFieldPermissions(badRecord, null, ReadPermission.class, "field7");
    }

    /**
     * Verifies ANY where all fail.
     */
    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testField8PermissionNotOk() {
        final PermissionExecutor permissionExecutor = new ActivePermissionExecutor(badRecord.getRequestScope());
        permissionExecutor.checkSpecificFieldPermissions(badRecord, null, ReadPermission.class, "field8");
    }
}
