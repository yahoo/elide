/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async;

import com.paiondata.elide.async.models.ResultType;

/**
 * Default {@link ResultTypeFileExtensionMapper}.
 */
public class DefaultResultTypeFileExtensionMapper implements ResultTypeFileExtensionMapper {
    @Override
    public String getFileExtension(String resultType) {
        switch (resultType) {
        case ResultType.JSON:
            return ".json";
        case ResultType.CSV:
            return ".csv";
        case ResultType.XLSX:
            return ".xlsx";
        default:
            return "";
        }
    }
}
