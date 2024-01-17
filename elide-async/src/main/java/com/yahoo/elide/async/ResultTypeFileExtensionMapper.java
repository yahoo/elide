/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async;

import com.yahoo.elide.async.models.ResultType;

/**
 * Determines the file extension from the {@link ResultType}.
 */
public interface ResultTypeFileExtensionMapper {
    String getFileExtension(String resultType);
}
