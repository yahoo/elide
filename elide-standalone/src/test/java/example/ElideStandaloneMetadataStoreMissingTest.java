/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yahoo.elide.core.utils.ClassScanner;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.yahoo.elide.modelconfig.DynamicConfiguration;
import com.yahoo.elide.standalone.ElideStandalone;
import com.yahoo.elide.standalone.config.ElideStandaloneAnalyticSettings;
import org.eclipse.jetty.util.MultiException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Optional;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ElideStandaloneMetadataStoreMissingTest {

    private ElideStandalone elide;

    @BeforeAll
    public void init() throws Exception {
        elide = new ElideStandalone(new ElideStandaloneTestSettings() {

            @Override
            public ElideStandaloneAnalyticSettings getAnalyticProperties() {
                ElideStandaloneAnalyticSettings analyticProperties = new ElideStandaloneAnalyticSettings() {
                    @Override
                    public boolean enableDynamicModelConfig() {
                        return true;
                    }

                    @Override
                    public boolean enableAggregationDataStore() {
                        return true;
                    }

                    @Override
                    public String getDefaultDialect() {
                        return SQLDialectFactory.getDefaultDialect().getDialectType();
                    }

                    @Override
                    public String getDynamicConfigPath() {
                        return "src/test/resources/configs/";
                    }
                };
                return analyticProperties;
            }

            @Override
            public MetaDataStore getMetaDataStore(ClassScanner scanner, Optional<DynamicConfiguration> validator) {
                return null;
            }
        });
    }

    @AfterAll
    public void shutdown() throws Exception {
        elide.stop();
    }

    @Test
    public void testMetadataStoreMissing() {
       assertThrows(MultiException.class, () -> elide.start(false));
    }
}
