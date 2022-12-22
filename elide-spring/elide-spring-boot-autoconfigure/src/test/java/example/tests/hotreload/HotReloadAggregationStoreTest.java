/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example.tests.hotreload;

import example.tests.AggregationStoreTest;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(
        properties = {
                "management.endpoints.web.exposure.include: *"
        }
)
public class HotReloadAggregationStoreTest extends AggregationStoreTest {

    @Override
    @BeforeAll
    public void setUp() {
        super.setUp();
        refreshServer();
    }
}
