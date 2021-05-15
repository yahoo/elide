/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.modelconfig.validator;

import static com.yahoo.elide.modelconfig.validator.DynamicConfigValidator.validateNameUniqueness;
import static com.yahoo.elide.modelconfig.validator.DynamicConfigValidator.validateTableSource;
import static com.yahoo.elide.modelconfig.validator.TableArgumentValidator.verifyDefaultValue;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.modelconfig.model.Column;
import com.yahoo.elide.modelconfig.model.ElideTableConfig;
import com.yahoo.elide.modelconfig.model.Table;


import lombok.AllArgsConstructor;

@AllArgsConstructor
/**
 * Verifies all defined arguments for column.
 * Ensures all arguments required for column are defined.
 */
public class ColumnArgumentValidator {

    private final ElideTableConfig elideTableConfig;
    private final EntityDictionary dictionary;
    private final Table table;
    private final Column column;
    private final String errorMsgPrefix;

    public ColumnArgumentValidator(ElideTableConfig elideTableConfig, EntityDictionary dictionary, Table table,
                    Column column) {
        this.elideTableConfig = elideTableConfig;
        this.dictionary = dictionary;
        this.table = table;
        this.column = column;
        this.errorMsgPrefix = String.format("Failed to verify column arguments for column: %s in table: %s. ",
                        column.getName(), table.getGlobalName());
    }

    public void validate() {
        validateNameUniqueness(table.getArguments(), String.format("For table: %s and column: %s, Multiple Column"
                                                                   + " Arguments found with the same name: ",
                                                                   table.getGlobalName(), column.getName()));

        column.getArguments().forEach(arg -> {
            verifyDefaultValue(arg, errorMsgPrefix);
            validateTableSource(elideTableConfig, dictionary, arg.getTableSource());
        });
    }
}
