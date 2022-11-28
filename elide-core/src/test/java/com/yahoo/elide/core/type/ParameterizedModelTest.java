/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.type;

import static com.yahoo.elide.core.type.ClassType.STRING_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.spy;

import com.yahoo.elide.core.exceptions.InvalidParameterizedAttributeException;
import com.yahoo.elide.core.request.Attribute;
import org.junit.jupiter.api.Test;

public class ParameterizedModelTest {

    @Test
    public void testInvoke() {
        ParameterizedModel testModel = spy(ParameterizedModel.class);
        Attribute testAttribute = Attribute.builder().type(STRING_TYPE).name("foo").build();
        String testValue = "bar";

        testModel.addAttributeValue(testAttribute, testValue);

        assertEquals(testValue, testModel.invoke(testAttribute));
    }

    @Test
    public void testInvokeException() {
        ParameterizedModel testModel = spy(ParameterizedModel.class);
        Attribute testAttribute = Attribute.builder().type(STRING_TYPE).name("foo").build();

        Exception exception = assertThrows(InvalidParameterizedAttributeException.class,
                () -> testModel.invoke(testAttribute));

        assertEquals("No attribute found with matching parameters for attribute: Attribute(name=foo)",
                exception.getMessage());
    }

    @Test
    public void testFetch() {
        ParameterizedModel testModel = spy(ParameterizedModel.class);
        Attribute testAttribute = Attribute.builder().type(STRING_TYPE).name("foo").build();
        String testValue = "bar";

        testModel.addAttributeValue(testAttribute, testValue);

        assertEquals(testValue, testModel.fetch(testAttribute.getAlias(), "blah"));
    }

    @Test
    public void testFetchDefault() {
        ParameterizedModel testModel = spy(ParameterizedModel.class);
        Attribute testAttribute = Attribute.builder().type(STRING_TYPE).name("foo").build();
        String testValue = "blah";

        assertEquals(testValue, testModel.fetch(testAttribute.getAlias(), testValue));
    }
}
