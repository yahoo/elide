/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.extension.runtime;

import com.yahoo.elide.core.utils.ClassScanner;
import com.yahoo.elide.core.utils.DefaultClassScanner;
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

    public static final Class[] ANNOTATIONS = {
        com.yahoo.elide.annotation.Include.class,
        com.yahoo.elide.annotation.SecurityCheck.class,
        com.yahoo.elide.annotation.LifeCycleHookBinding.class,
        com.yahoo.elide.core.utils.coerce.converters.ElideTypeConverter.class,
        com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable.class,
        com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery.class,
        javax.persistence.Entity.class,
        javax.persistence.Table.class,
    };

    public BeanContainerListener setElideConfig(ElideConfig config) {
        return beanContainer -> {
            ElideBeans elideBeans = beanContainer.instance(ElideBeans.class);
            elideBeans.setElideConfig(config);
        };
    }

    public Supplier<ClassScanner> createClassScanner(List<Class<?>> classes) {
        return new Supplier<ClassScanner>() {
            @Override
            public ClassScanner get() {
                System.out.println("Scanning classes");
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
