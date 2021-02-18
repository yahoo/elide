/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.yahoo.elide.jsonapi.links.DefaultJSONApiLinks;
import com.yahoo.elide.standalone.config.ElideStandaloneAnalyticSettings;
import com.yahoo.elide.standalone.config.ElideStandaloneAsyncSettings;
import com.yahoo.elide.standalone.config.ElideStandaloneSettings;

import example.models.Post;

import java.util.Properties;
import java.util.TimeZone;

/**
 * Settings class extending ElideStandaloneSettings for tests.
 */
public class ElideStandaloneTestSettings implements ElideStandaloneSettings {

    @Override
    public ElideSettings getElideSettings(EntityDictionary dictionary, DataStore dataStore) {
        String jsonApiBaseUrl = getBaseUrl()
                + getJsonApiPathSpec().replaceAll("/\\*", "")
                + "/";

        ElideSettingsBuilder builder = new ElideSettingsBuilder(dataStore)
                .withEntityDictionary(dictionary)
                .withJoinFilterDialect(new RSQLFilterDialect(dictionary))
                .withSubqueryFilterDialect(new RSQLFilterDialect(dictionary))
                .withJSONApiLinks(new DefaultJSONApiLinks(jsonApiBaseUrl))
                .withBaseUrl("https://elide.io")
                .withAuditLogger(getAuditLogger())
                .withJsonApiPath(getJsonApiPathSpec().replaceAll("/\\*", ""))
                .withGraphQLApiPath(getGraphQLApiPathSpec().replaceAll("/\\*", ""))
                .withExportApiPath(getAsyncProperties().getExportApiPathSpec().replaceAll("/\\*", ""));

        if (enableISO8601Dates()) {
            builder = builder.withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"));
        }

        return builder.build();
    }

    @Override
    public String getBaseUrl() {
        return "https://elide.io";
    }

    @Override
    public Properties getDatabaseProperties() {
        Properties options = new Properties();

        options.put("hibernate.show_sql", "true");
        options.put("hibernate.hbm2ddl.auto", "create");
        options.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        options.put("hibernate.current_session_context_class", "thread");
        options.put("hibernate.jdbc.use_scrollable_resultset", "true");

        options.put("javax.persistence.jdbc.driver", "org.h2.Driver");
        options.put("javax.persistence.jdbc.url", "jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1;");
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
    public ElideStandaloneAsyncSettings getAsyncProperties() {
        ElideStandaloneAsyncSettings asyncPropeties = new ElideStandaloneAsyncSettings() {
            @Override
            public boolean enabled() {
                return true;
            }

            @Override
            public boolean enableCleanup() {
                return true;
            }

            @Override
            public Integer getThreadSize() {
                return 5;
            }

            @Override
            public Integer getMaxRunTimeSeconds() {
                return 1800;
            }

            @Override
            public Integer getQueryCleanupDays() {
                return 3;
            }

            @Override
            public boolean enableExport() {
                return false;
            }
        };
        return asyncPropeties;
    }

    @Override
    public ElideStandaloneAnalyticSettings getAnalyticProperties() {
        ElideStandaloneAnalyticSettings analyticPropeties = new ElideStandaloneAnalyticSettings() {
            @Override
            public boolean enableDynamicModelConfig() {
                return true;
            }

            @Override
            public boolean enableAggregationDataStore() {
                return true;
            }

            @Override
            public boolean enableMetaDataStore() {
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
        return analyticPropeties;
    }
}
