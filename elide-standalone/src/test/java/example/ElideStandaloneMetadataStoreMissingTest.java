/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.paiondata.elide.core.utils.ClassScanner;
import com.paiondata.elide.datastores.aggregation.metadata.MetaDataStore;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.paiondata.elide.modelconfig.DynamicConfiguration;
import com.paiondata.elide.standalone.ElideStandalone;
import com.paiondata.elide.standalone.config.ElideStandaloneAnalyticSettings;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import jakarta.servlet.ServletException;

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

    private void setLoggingLevel(Level level) {
        Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        logger.setLevel(level);
    }

    private Level getLoggingLevel() {
        return ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).getLevel();
    }

    @AfterAll
    public void shutdown() throws Exception {
        elide.stop();
    }

    @Test
    public void testMetadataStoreMissing() {
        Level old = getLoggingLevel();

        try {
            setLoggingLevel(Level.OFF);
            assertThrows(ServletException.class, () -> elide.start(false));
        } finally {
            setLoggingLevel(old);
        }
    }
}
