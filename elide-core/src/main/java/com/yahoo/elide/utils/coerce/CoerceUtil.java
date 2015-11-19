/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.utils.coerce;

import com.yahoo.elide.core.exceptions.InvalidAttributeException;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.utils.coerce.converters.ToEnumConverter;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.Converter;

/**
 * Class for coercing a value to a target class.
 */
public class CoerceUtil {

    private static final ToEnumConverter ENUM_CONVERTER = new ToEnumConverter();

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
    public static Object coerce(Object value, Class<?> cls) {

        if (value == null || cls == null || cls.isAssignableFrom(value.getClass())) {
            return value;
        }

        try {
            return ConvertUtils.convert(value, cls);

        } catch (ConversionException | InvalidAttributeException e) {
            throw new InvalidValueException(value);
        }
    }

    /**
     * Perform CoerceUtil setup.
     *
     */
    private static void setup() {
        BeanUtilsBean.setInstance(new BeanUtilsBean(new ConvertUtilsBean() {

            //Overriding lookup to execute enum converter if target is enum
            @Override
            public Converter lookup(Class<?> sourceType, Class<?> targetType) {
                if (targetType.isEnum()) {
                    return ENUM_CONVERTER;
                } else {
                    return super.lookup(sourceType, targetType);
                }
            }
        }));
    }
}
