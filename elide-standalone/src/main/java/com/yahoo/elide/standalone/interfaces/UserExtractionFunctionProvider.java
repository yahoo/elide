/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.standalone.interfaces;

import com.yahoo.elide.resources.DefaultOpaqueUserFunction;

/**
 * User extraction function provider.
 */
public interface UserExtractionFunctionProvider {

    /**
     * User extraction function provider.
     *
     * @return User extraction function.
     */
    DefaultOpaqueUserFunction getUserExtractionFunction();
}
