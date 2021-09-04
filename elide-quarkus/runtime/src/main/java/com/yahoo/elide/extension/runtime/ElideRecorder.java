package com.yahoo.elide.extension.runtime;

import com.yahoo.elide.core.utils.ClassScanner;
import com.yahoo.elide.core.utils.DefaultClassScanner;
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

    private static final Class[] ANNOTATIONS = {
        com.yahoo.elide.annotation.Include.class,
        com.yahoo.elide.annotation.SecurityCheck.class,
        com.yahoo.elide.core.utils.coerce.converters.ElideTypeConverter.class
    };

    public Supplier<ClassScanner> createClassScanner(List<Class<?>> classes) {
        return new Supplier<ClassScanner>() {
            @Override
            public ClassScanner get() {
                Map<String, Set<Class<?>>> cache = new HashMap<>();
                Arrays.stream(ANNOTATIONS).forEach(annotationClass -> {
                    classes.stream().forEach(cls -> {
                        if (cls.isAnnotationPresent(annotationClass)) {
                            String key = annotationClass.getCanonicalName();

                            Set<Class<?>> classSet = cache.getOrDefault(key, new HashSet<>());
                            classSet.add(cls);
                            cache.put(key, classSet);
                        }
                    });
                });

                return new DefaultClassScanner(cache);
            }
        };
    }
}
