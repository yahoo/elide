/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import example.Job;
import org.junit.jupiter.api.Test;

import hooks.JobLifeCycleHook;

/**
 * Test lifecycle hooks.
 */
public class LifecycleHookTest extends PersistentResourceFetcherTest {

    protected JobLifeCycleHook.JobService jobService = mock(JobLifeCycleHook.JobService.class);

    @Test
    public void testCreate() throws Exception {
        String query = "mutation {\n"
                + "  job(op: UPSERT, data: {id: 1} ) {\n"
                + "    edges {\n"
                + "      node {\n"
                + "        id\n"
                + "        status\n"
                + "        result\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}";

        assertQueryEquals(query, "{\"job\":{\"edges\":[{\"node\":{\"id\":\"1\",\"status\":1,\"result\":\"Pending\"}}]}}");
    }

    @Test
    public void testUpdate() throws Exception {
        testCreate();

        String query = "mutation {\n"
                + "  job(op: UPDATE, data: { id: \"1\", result: \"Done\" } ) {\n"
                + "    edges {\n"
                + "      node {\n"
                + "        id\n"
                + "        status\n"
                + "        result\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}";

        assertQueryEquals(query,
                "{\"job\":{\"edges\":[{\"node\":{\"id\":\"1\",\"status\":2, \"result\":\"Done\"}}]}}");
    }

    @Test
    public void testDelete() throws Exception {
        testCreate();

        String query = "mutation {\n"
                + "  job(op: DELETE, ids: [\"1\"]) {\n"
                + "    edges {\n"
                + "      node {\n"
                + "        id\n"
                + "        status\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}";

        assertQueryEquals(query, "{\"job\":{\"edges\":[]}}");
        verify(jobService, times(1)).jobDeleted(any(Job.class));
    }

    @Override
    protected void initializeMocks() {
        super.initializeMocks();
        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            Object entity = args[0];

            ((JobLifeCycleHook) entity).setJobService(jobService);

            //void method so return null
            return null;
        }).when(injector).inject(any(JobLifeCycleHook.class));
    }
}
