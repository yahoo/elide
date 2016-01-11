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

import static org.testng.Assert.*;

public class SpecificFieldTest {

    @Test(expectedExceptions = {ForbiddenAccessException.class})
    public void testFailRun() throws Exception {
        new SpecificField().run(false, false, true, true);
    }

    @Test
    public void testSuccessRun() throws Exception {
        new SpecificField().run(true, false, true, true);
    }

    @Test
    public void testShouldContinueUponEntitySuccess() throws Exception {
        Assert.assertTrue(new SpecificField().shouldContinueUponEntitySuccess(PermissionManager.CheckMode.ALL));
        Assert.assertTrue(new SpecificField().shouldContinueUponEntitySuccess(PermissionManager.CheckMode.ANY));
    }

    @Test
    public void testShouldContinueUponFieldFailure() throws Exception {
        // Remember: This strategy should be used _ONLY_ in the case of a single field to check
        Assert.assertFalse(new SpecificField().shouldContinueUponFieldFailure(PermissionManager.CheckMode.ALL, true));
        Assert.assertFalse(new SpecificField().shouldContinueUponFieldFailure(PermissionManager.CheckMode.ALL, false));
        Assert.assertTrue(new SpecificField().shouldContinueUponFieldFailure(PermissionManager.CheckMode.ANY, true));
        Assert.assertFalse(new SpecificField().shouldContinueUponFieldFailure(PermissionManager.CheckMode.ANY, false));
    }
}
