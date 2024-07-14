/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.validator;

import com.yahoo.elide.core.dictionary.Injector;
import com.yahoo.elide.core.utils.ClassScanner;
import com.yahoo.elide.datastores.aggregation.DefaultQueryValidator;
import com.yahoo.elide.datastores.aggregation.metadata.FormulaValidator;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.query.DefaultQueryPlanMerger;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;
import com.yahoo.elide.modelconfig.store.models.ConfigFile;
import com.yahoo.elide.modelconfig.validator.DynamicConfigValidator;
import com.yahoo.elide.modelconfig.validator.Validator;

import java.util.HashSet;
import java.util.Map;

/**
 * Delegates to DynamicConfigValidator for HJSON syntax and JSON schema validation.  Then builds
 * table metadata to perform template validation.
 */
public class TemplateConfigValidator implements Validator {

    private final ClassScanner scanner;
    private final String configRoot;
    private final Injector injector;

    public TemplateConfigValidator(
            ClassScanner scanner,
            Injector injector,
            String configRoot
    ) {
        this.scanner = scanner;
        this.injector = injector;
        this.configRoot = configRoot;
    }

    //Rebuilds the MetaDataStore for each validation so that we can validate templates.
    MetaDataStore rebuildMetaDataStore(Map<String, ConfigFile> resourceMap) {
        DynamicConfigValidator validator = new DynamicConfigValidator(
                entityDictionaryBuilder -> entityDictionaryBuilder.scanner(scanner).injector(injector), configRoot);

        validator.validate(resourceMap);

        MetaDataStore metaDataStore = new MetaDataStore(scanner, injector, validator.getTables(),
                validator.getNamespaceConfigurations(), false);

        //Populates the metadata store with SQL tables.
        new SQLQueryEngine(metaDataStore, (unused) -> null, new HashSet<>(),
                new DefaultQueryPlanMerger(metaDataStore),
                new DefaultQueryValidator(metaDataStore.getMetadataDictionary()));

        return metaDataStore;
    }

    @Override
    public void validate(Map<String, ConfigFile> resourceMap) {
        MetaDataStore metaDataStore = rebuildMetaDataStore(resourceMap);

        metaDataStore.getTables().forEach(table -> {
            SQLTable sqlTable = (SQLTable) table;

            checkForCycles(sqlTable, metaDataStore);

            TableArgumentValidator tableArgValidator = new TableArgumentValidator(metaDataStore, sqlTable);
            tableArgValidator.validate();

            sqlTable.getAllColumns().forEach(column -> {
                ColumnArgumentValidator colArgValidator = new ColumnArgumentValidator(metaDataStore, sqlTable, column);
                colArgValidator.validate();
            });
        });
    }

    /**
     * Verify that there is no reference loop for given {@link SQLTable}.
     * @param sqlTable Queryable to validate.
     */
    private void checkForCycles(SQLTable sqlTable, MetaDataStore metaDataStore) {
        FormulaValidator formulaValidator = new FormulaValidator(metaDataStore);
        sqlTable.getColumnProjections().forEach(column -> formulaValidator.parse(sqlTable, column));
    }
}
