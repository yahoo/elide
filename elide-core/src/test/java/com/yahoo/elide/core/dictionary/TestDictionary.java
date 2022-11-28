/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.dictionary;

import com.yahoo.elide.core.security.checks.Check;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.core.utils.DefaultClassScanner;
import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import example.TestCheckMappings;

import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Test Entity Dictionary.
 */
@Singleton
public class TestDictionary extends EntityDictionary {

    @Inject
    public TestDictionary(Injector injector,
                          @Named("checkMappings") Map<String, Class<? extends Check>> checks) {
        super(
                checks,
                Collections.emptyMap(), //role Checks
                injector,
                CoerceUtil::lookup,
                Collections.emptySet(),
                DefaultClassScanner.getInstance()
        );
    }

    @Override
    public Type<?> lookupBoundClass(Type<?> objClass) {
        // Special handling for mocked Book class which has Entity annotation
        if (objClass.getName().contains("$MockitoMock$")) {
            objClass = objClass.getSuperclass();
        }
        return super.lookupBoundClass(objClass);
    }

    /**
     * Returns a test dictionary injected with Guice.
     * @return a test dictionary.
     */
    public static EntityDictionary getTestDictionary() {
        return getTestDictionary(TestCheckMappings.MAPPINGS);
    }

    /**
     * Returns a test dictionary injected with Guice.
     * @param checks The security checks to setup the dictionary with.
     * @return a test dictionary.
     */
    public static EntityDictionary getTestDictionary(Map<String, Class<? extends Check>> checks) {
        return Guice.createInjector(new Module() {
            @Override
            public void configure(Binder binder) {
                binder.bind(Injector.class).to(TestInjector.class);
                binder.bind(EntityDictionary.class).to(TestDictionary.class);
                binder.bind(new TypeLiteral<Map<String, Class<? extends Check>>>() { })
                      .annotatedWith(Names.named("checkMappings"))
                      .toInstance(checks);
            }
        }).getInstance(EntityDictionary.class);
    }
}
