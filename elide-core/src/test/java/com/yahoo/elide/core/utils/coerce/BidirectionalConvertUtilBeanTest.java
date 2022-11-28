/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.utils.coerce;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.apache.commons.beanutils.Converter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Currency;

public class BidirectionalConvertUtilBeanTest {

    private static BidirectionalConvertUtilBean convertUtilBean;

    @BeforeAll
    public static void init() {
        convertUtilBean = new BidirectionalConvertUtilBean();
        convertUtilBean.register(String.class, Currency.class, new Converter() {
            @Override
            public <T> T convert(Class<T> aClass, Object o) {
                return null;
            }
        });
    }

    @Test
    public void testSourceTargetLookup() {
        assertNotNull(convertUtilBean.lookup(String.class, Currency.class));
    }

    @Test
    public void testInvalidLookup() {
        assertNull(convertUtilBean.lookup(Long.class, Currency.class));
    }
}
