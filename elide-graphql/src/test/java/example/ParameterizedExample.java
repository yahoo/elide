/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.paiondata.elide.annotation.Include;
import com.paiondata.elide.core.type.ParameterizedModel;

import jakarta.persistence.Id;

@Include
public class ParameterizedExample extends ParameterizedModel {

    @Id
    long id;

    private String attribute;
}
