/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.extension.test;

import org.junit.jupiter.api.Assertions;

public class ElideExtensionDevModeTest {

    // Start hot reload (DevMode) test with your extension loaded
    //@RegisterExtension
    //static final QuarkusDevModeTest devModeTest = new QuarkusDevModeTest()
    //    .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    //@Test
    public void writeYourOwnDevModeTest() {
        // Write your dev mode tests here - see the testing extension guide https://quarkus.io/guides/writing-extensions#testing-hot-reload for more information
        Assertions.assertTrue(true, "Add dev mode assertions to " + getClass().getName());
    }
}
