/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.extension.runtime;

import com.paiondata.elide.core.utils.ClassScanner;
import com.paiondata.elide.core.utils.DefaultClassScanner;
import org.jboss.logging.Logger;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.runtime.annotations.Recorder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

@Recorder
public class ElideRecorder {
    private static final Logger LOG = Logger.getLogger(ElideRecorder.class.getName());

    public static final Class[] ANNOTATIONS = {
        com.paiondata.elide.annotation.Include.class,
        com.paiondata.elide.annotation.SecurityCheck.class,
        com.paiondata.elide.annotation.LifeCycleHookBinding.class,
        com.paiondata.elide.core.utils.coerce.converters.ElideTypeConverter.class,
        com.paiondata.elide.datastores.aggregation.queryengines.sql.annotation.FromTable.class,
        com.paiondata.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery.class,
        jakarta.persistence.Entity.class,
        jakarta.persistence.Table.class,
    };

    public BeanContainerListener setElideConfig(ElideConfig config) {
        return beanContainer -> {
            ElideBeans elideBeans = beanContainer.beanInstance(ElideBeans.class);
            elideBeans.setElideConfig(config);
        };
    }

    public Supplier<ClassScanner> createClassScanner(List<Class<?>> classes) {
        return new Supplier<ClassScanner>() {
            @Override
            public ClassScanner get() {
                LOG.debug("Scanning Classes");
                Map<String, Set<Class<?>>> cache = new HashMap<>();
                Arrays.stream(ANNOTATIONS).forEach(annotationClass -> {
                    String key = annotationClass.getCanonicalName();
                    cache.put(key, new HashSet<>());

                    classes.stream().forEach(cls -> {
                        if (cls.isAnnotationPresent(annotationClass)) {
                            Set<Class<?>> classSet = cache.get(key);
                            classSet.add(cls);
                        }
                    });
                });

                return new DefaultClassScanner(cache);
            }
        };
    }
}
