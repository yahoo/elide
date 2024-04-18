/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.export.formatter;

import com.paiondata.elide.async.export.formatter.TableExportFormatters.TableExportFormattersBuilder;

/**
 * Used to customize the mutable {@link TableExportFormattersBuilder}.
 */
public interface TableExportFormattersBuilderCustomizer {
    void customize(TableExportFormattersBuilder builder);
}
