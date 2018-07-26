/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.utils.coerce;

import com.yahoo.elide.core.exceptions.InvalidAttributeException;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.utils.coerce.converters.EpochToDateConverter;
import com.yahoo.elide.utils.coerce.converters.FromMapConverter;
import com.yahoo.elide.utils.coerce.converters.ToEnumConverter;
import com.yahoo.elide.utils.coerce.converters.ToUUIDConverter;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.lang3.ClassUtils;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Class for coercing a value to a target class.
 */
public class CoerceUtil {

    private static final ToEnumConverter TO_ENUM_CONVERTER = new ToEnumConverter();
    private static final ToUUIDConverter TO_UUID_CONVERTER = new ToUUIDConverter();
    private static final FromMapConverter FROM_MAP_CONVERTER = new FromMapConverter();
    private static final EpochToDateConverter EPOCH_TO_DATE_CONVERTER = new EpochToDateConverter();

    //static block for setup and registering new converters
    static {
        setup();
    }

    /**
     * Convert value to target class.
     *
     * @param <T> type
     * @param value value to convert
     * @param cls   class to convert to
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
     * Register a new type converter for Elide type coercion/deserialization
     * @param converter  The converter
     * @param targetType The type that needs coercion
     * @param <T> The type that needs coercion
     */
    public static <T> void register(Converter converter, Class<T> targetType) {
        ConvertUtils.register(converter, targetType);
    }

    /**
     * Perform CoerceUtil setup.
     */
    private static void setup() {
        BeanUtilsBean.setInstance(new BeanUtilsBean(new ConvertUtilsBean() {
            {
                // https://github.com/yahoo/elide/issues/260
                // enable throwing exceptions when conversion fails
                register(true, false, 0);

                register(TO_UUID_CONVERTER, UUID.class);
                register(EPOCH_TO_DATE_CONVERTER, Date.class);
            }

            @Override
            /*
             * Overriding lookup to execute enum converter if target is enum
             * or map convert if source is map
             */
            public Converter lookup(Class<?> sourceType, Class<?> targetType) {
                if (targetType.isEnum()) {
                    return TO_ENUM_CONVERTER;
                } else if (Map.class.isAssignableFrom(sourceType)) {
                    return FROM_MAP_CONVERTER;
                } else if ((String.class.isAssignableFrom(sourceType) || Number.class.isAssignableFrom(sourceType))
                        && ClassUtils.isAssignable(targetType, Date.class)
                        && super.lookup(Date.class).equals(EPOCH_TO_DATE_CONVERTER)) {
                    return EPOCH_TO_DATE_CONVERTER;
                } else {
                    return super.lookup(sourceType, targetType);
                }
            }
        }));
    }
}
