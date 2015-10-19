/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
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
    private User badUser;
    private EntityDictionary dictionary;

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

        funRecord = new PersistentResource<>(fun, new RequestScope(null, null, goodUser, dictionary, null));
        badRecord = new PersistentResource<>(fun, new RequestScope(null, null, badUser, dictionary, null));
    }

    @Test
    public void testClassAnyOk() {
        PersistentResource.checkPermission(ReadPermission.class, funRecord);
        PersistentResource.checkPermission(UpdatePermission.class, funRecord);
        PersistentResource.checkPermission(CreatePermission.class, funRecord);
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testClassAllNotOk() {
        PersistentResource.checkPermission(DeletePermission.class, funRecord);
    }

    @Test
    public void testFieldPermissionOk() {
        PersistentResource.checkFieldPermission(ReadPermission.class, funRecord, "field3");
        PersistentResource.checkFieldPermission(ReadPermission.class, funRecord, "relation1");
        PersistentResource.checkFieldPermission(ReadPermission.class, funRecord, "relation2");
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testField3PermissionNotOk() {
        PersistentResource.checkFieldPermission(ReadPermission.class, badRecord, "field3");
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testRelation1PermissionNotOk() {
        PersistentResource.checkFieldPermission(ReadPermission.class, badRecord, "relation1");
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testRelation2PermissionNotOk() {
        PersistentResource.checkFieldPermission(ReadPermission.class, badRecord, "relation2");
    }

    /**
     * Verifies ANY where the first fails but the last succeeds.
     */
    @Test()
    public void testField5PermissionOk() {
        PersistentResource.checkFieldPermission(ReadPermission.class, badRecord, "field5");
    }

    /**
     * Verifies ALL where the first fails.
     */
    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testField6PermissionNotOk() {
        PersistentResource.checkFieldPermission(ReadPermission.class, badRecord, "field6");
    }

    /**
     * Verifies ALL where the last fails.
     */
    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testField7PermissionNotOk() {
        PersistentResource.checkFieldPermission(ReadPermission.class, badRecord, "field7");
    }

    /**
     * Verifies ANY where all fail.
     */
    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testField8PermissionNotOk() {
        PersistentResource.checkFieldPermission(ReadPermission.class, badRecord, "field8");
    }
}
