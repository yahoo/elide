/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.utils.coerce;

import com.yahoo.elide.core.exceptions.InvalidAttributeException;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.utils.coerce.converters.ElideConverter;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.ConvertUtils;

/**
 * Class for coercing a value to a target class.
 */
public class CoerceUtil {

    //static block for setup and registering new converters
    static {
        setup();
    }

    /**
     * Convert value to target class.
     *
     * @param value value to convert
     * @param cls class to convert to
     * @return coerced value
     */
    public static <T> T coerce(Object value, Class<T> cls) {
        if (value == null || cls == null || cls.isAssignableFrom(value.getClass())) {
            return (T) value;
        }

        try {
            return (T) ConvertUtils.convert(value, cls);
        } catch (ConversionException | InvalidAttributeException | IllegalArgumentException e) {
            throw new InvalidValueException(value, e.getMessage());
        }
    }

    /**
     * Perform CoerceUtil setup.
     */
    private static void setup() {
        BeanUtilsBean.setInstance(new BeanUtilsBean(new ElideConverter()));
    }
}
