/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example.tests.hotreload;

import example.tests.SubscriptionTest;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(
        properties = {
                "management.endpoints.web.exposure.include: *"
        }
)
@Import(SubscriptionTest.SerdeConfiguration.class)
public class HotReloadSubscriptionTest extends SubscriptionTest {

    @Override
    @BeforeAll
    public void setUp() {
        super.setUp();
        refreshServer();
    }
}
