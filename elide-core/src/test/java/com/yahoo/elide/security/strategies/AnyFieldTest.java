/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.strategies;

import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.security.PermissionManager;
import org.testng.Assert;
import org.testng.annotations.Test;

public class AnyFieldTest {

    @Test
    public void shouldContinueUponFieldFailure() throws Exception {
        // Should simply always return true
        Assert.assertTrue(new AnyField().shouldContinueUponFieldFailure(null, true));
        Assert.assertTrue(new AnyField().shouldContinueUponFieldFailure(null, false));
        Assert.assertTrue(new AnyField().shouldContinueUponFieldFailure(PermissionManager.CheckMode.ALL, true));
        Assert.assertTrue(new AnyField().shouldContinueUponFieldFailure(PermissionManager.CheckMode.ALL, true));
        Assert.assertTrue(new AnyField().shouldContinueUponFieldFailure(PermissionManager.CheckMode.ANY, false));
        Assert.assertTrue(new AnyField().shouldContinueUponFieldFailure(PermissionManager.CheckMode.ANY, false));
    }

    @Test
    public void testShouldContinueUponEntitySuccess() throws Exception {
        Assert.assertFalse(new AnyField().shouldContinueUponEntitySuccess(PermissionManager.CheckMode.ANY));
        Assert.assertTrue(new AnyField().shouldContinueUponEntitySuccess(PermissionManager.CheckMode.ALL));
        Assert.assertTrue(new AnyField().shouldContinueUponEntitySuccess(null));
    }

    @Test
    public void testSuccessRun() {
        new AnyField().run(true, false, false, false);
    }

    @Test(expectedExceptions = {ForbiddenAccessException.class})
    public void testFailRun() throws Exception {
        new AnyField().run(false, false, true, true);
    }
}
