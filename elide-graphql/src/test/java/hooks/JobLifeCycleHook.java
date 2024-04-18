/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package hooks;

import com.paiondata.elide.annotation.LifeCycleHookBinding;
import com.paiondata.elide.core.lifecycle.LifeCycleHook;
import com.paiondata.elide.core.security.ChangeSpec;
import com.paiondata.elide.core.security.RequestScope;
import example.Job;

import jakarta.inject.Inject;
import lombok.Setter;

import java.util.Optional;

/**
 * Tests a hooks in GraphQL.
 */
public class JobLifeCycleHook implements LifeCycleHook<Job> {

    public interface JobService {
        void jobDeleted(Job job);
    }

    @Inject
    @Setter
    private JobService jobService;


    @Override
    public void execute(
            LifeCycleHookBinding.Operation operation,
            LifeCycleHookBinding.TransactionPhase phase,
            Job job, RequestScope requestScope,
            Optional<ChangeSpec> changes
    ) {
        switch (operation) {
            case DELETE: {
                jobService.jobDeleted(job);
            }
            case UPDATE: {
                job.setStatus(2);
                return;
            }
            case CREATE: {
                if (phase == LifeCycleHookBinding.TransactionPhase.PRESECURITY) {
                    job.setResult("Pending");
                } else {
                    job.setStatus(1);
                }
            }
        };
    }
}
