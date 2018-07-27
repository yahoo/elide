/*
 * Copyright 2018, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.utils.coerce;

import org.apache.commons.beanutils.Converter;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Currency;

public class BidirectionalConvertUtilBeanTest {

    private BidirectionalConvertUtilBean convertUtilBean;

    @BeforeClass
    public void init() {
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
        Assert.assertNotNull(convertUtilBean.lookup(String.class, Currency.class));
    }

    @Test
    public void testInvalidLookup() {
        Assert.assertNull(convertUtilBean.lookup(Long.class, Currency.class));
    }
}
