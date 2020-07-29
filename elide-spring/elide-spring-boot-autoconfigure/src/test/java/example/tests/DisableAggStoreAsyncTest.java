/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.tests;

import org.springframework.test.context.ActiveProfiles;

/**
 * Executes Async tests with Aggregation Store disabled.
 */
@ActiveProfiles("disableAggStore")
public class DisableAggStoreAsyncTest extends AsyncTest {

}
