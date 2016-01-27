/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.audit.Logger;
import com.yahoo.elide.audit.TestLogger;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.security.PermissionExecutor;
import com.yahoo.elide.security.User;

import example.FunWithPermissions;
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
        dictionary = new EntityDictionary();
    }

    @BeforeTest
    public void setup() {
        dictionary.bindEntity(FunWithPermissions.class);

        FunWithPermissions fun = new FunWithPermissions();
        fun.setId(1);

        Logger testLogger = new TestLogger();
        funRecord = new PersistentResource<>(fun, new RequestScope(null, null, goodUser, dictionary, null, testLogger));
        badRecord = new PersistentResource<>(fun, new RequestScope(null, null, badUser, dictionary, null, testLogger));
    }

    @Test
    public void testClassAnyOk() {
        final PermissionExecutor permissionExecutor = new PermissionExecutor(funRecord.getRequestScope());
        permissionExecutor.checkPermission(ReadPermission.class, funRecord);
        permissionExecutor.checkPermission(UpdatePermission.class, funRecord);
        permissionExecutor.checkPermission(CreatePermission.class, funRecord);
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testClassAllNotOk() {
        final PermissionExecutor permissionExecutor = new PermissionExecutor(badRecord.getRequestScope());
        permissionExecutor.checkPermission(DeletePermission.class, funRecord);
    }

    @Test
    public void testFieldPermissionOk() {
        final PermissionExecutor permissionExecutor = new PermissionExecutor(funRecord.getRequestScope());
        permissionExecutor.checkSpecificFieldPermissions(funRecord, null, ReadPermission.class, "field3");
        permissionExecutor.checkSpecificFieldPermissions(funRecord, null, ReadPermission.class, "relation1");
        permissionExecutor.checkSpecificFieldPermissions(funRecord, null, ReadPermission.class, "relation2");
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testField3PermissionNotOk() {
        final PermissionExecutor permissionExecutor = new PermissionExecutor(badRecord.getRequestScope());
        permissionExecutor.checkSpecificFieldPermissions(badRecord, null, ReadPermission.class, "field3");
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testRelation1PermissionNotOk() {
        final PermissionExecutor permissionExecutor = new PermissionExecutor(badRecord.getRequestScope());
        permissionExecutor.checkSpecificFieldPermissions(badRecord, null, ReadPermission.class, "relation1");
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testRelation2PermissionNotOk() {
        final PermissionExecutor permissionExecutor = new PermissionExecutor(badRecord.getRequestScope());
        permissionExecutor.checkSpecificFieldPermissions(badRecord, null, ReadPermission.class, "relation2");
    }

    /**
     * Verifies ANY where the first fails but the last succeeds.
     */
    @Test()
    public void testField5PermissionOk() {
        final PermissionExecutor permissionExecutor = new PermissionExecutor(badRecord.getRequestScope());
        permissionExecutor.checkSpecificFieldPermissions(badRecord, null, ReadPermission.class, "field5");
    }

    /**
     * Verifies ALL where the first fails.
     */
    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testField6PermissionNotOk() {
        final PermissionExecutor permissionExecutor = new PermissionExecutor(badRecord.getRequestScope());
        permissionExecutor.checkSpecificFieldPermissions(badRecord, null, ReadPermission.class, "field6");
    }

    /**
     * Verifies ALL where the last fails.
     */
    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testField7PermissionNotOk() {
        final PermissionExecutor permissionExecutor = new PermissionExecutor(badRecord.getRequestScope());
        permissionExecutor.checkSpecificFieldPermissions(badRecord, null, ReadPermission.class, "field7");
    }

    /**
     * Verifies ANY where all fail.
     */
    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testField8PermissionNotOk() {
        final PermissionExecutor permissionExecutor = new PermissionExecutor(badRecord.getRequestScope());
        permissionExecutor.checkSpecificFieldPermissions(badRecord, null, ReadPermission.class, "field8");
    }
}
