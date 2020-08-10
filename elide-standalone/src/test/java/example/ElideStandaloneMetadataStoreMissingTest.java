/*
 * Copyright 2020, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yahoo.elide.async.service.AsyncQueryDAO;
import com.yahoo.elide.contrib.dynamicconfighelpers.compile.ElideDynamicEntityCompiler;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.standalone.ElideStandalone;
import com.yahoo.elide.standalone.config.ElideStandaloneSettings;
import example.models.Post;

import org.eclipse.jetty.util.MultiException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Optional;
import java.util.Properties;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ElideStandaloneMetadataStoreMissingTest {

    private ElideStandalone elide;

    @BeforeAll
    public void init() throws Exception {
        elide = new ElideStandalone(new ElideStandaloneSettings() {

            @Override
            public Properties getDatabaseProperties() {
                Properties options = new Properties();

                options.put("hibernate.show_sql", "true");
                options.put("hibernate.hbm2ddl.auto", "create");
                options.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
                options.put("hibernate.current_session_context_class", "thread");
                options.put("hibernate.jdbc.use_scrollable_resultset", "true");

                options.put("javax.persistence.jdbc.driver", "org.h2.Driver");
                options.put("javax.persistence.jdbc.url", "jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1;MVCC=TRUE;");
                options.put("javax.persistence.jdbc.user", "sa");
                options.put("javax.persistence.jdbc.password", "");
                return options;
            }

            @Override
            public String getModelPackageName() {
                return Post.class.getPackage().getName();
            }

            @Override
            public boolean enableSwagger() {
                return true;
            }

            @Override
            public boolean enableGraphQL() {
                return true;
            }

            @Override
            public boolean enableJSONAPI() {
                return true;
            }

            @Override
            public boolean enableAsync() {
                return true;
            }

            @Override
            public boolean enableAsyncCleanup() {
                return true;
            }

            @Override
            public Integer getAsyncThreadSize() {
                return 3;
            }

            @Override
            public Integer getAsyncMaxRunTimeMinutes() {
                return 30;
            }

            @Override
            public Integer getAsyncQueryCleanupDays() {
                return 3;
            }

            @Override
            public AsyncQueryDAO getAsyncQueryDAO() {
                return null;
            }

            @Override
            public boolean enableDynamicModelConfig() {
                return true;
            }

            @Override
            public boolean enableAggregationDataStore() {
                return true;
            }

            @Override
            public String getDynamicConfigPath() {
                return "src/test/resources/models/";
            }

            @Override
            public MetaDataStore getMetaDataStore(Optional<ElideDynamicEntityCompiler> optionalCompiler) {
                return null;
            }
        });
    }

    @Test
    public void testMetadataStoreMissing() {
       assertThrows(MultiException.class, () -> elide.start(false));
    }
}
