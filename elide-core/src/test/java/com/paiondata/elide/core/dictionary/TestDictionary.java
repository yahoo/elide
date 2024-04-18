/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.core.dictionary;

import com.paiondata.elide.core.security.checks.Check;
import com.paiondata.elide.core.type.Type;
import com.paiondata.elide.core.utils.DefaultClassScanner;
import com.paiondata.elide.core.utils.coerce.CoerceUtil;
import example.TestCheckMappings;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.util.Collections;
import java.util.Map;

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
                new DefaultClassScanner()
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
        return new TestDictionary(new TestInjector(), checks);
    }
}
