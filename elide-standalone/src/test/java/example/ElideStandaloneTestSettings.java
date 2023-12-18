/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.jsonapi.JsonApiSettings.JsonApiSettingsBuilder;
import com.yahoo.elide.jsonapi.links.DefaultJsonApiLinks;
import com.yahoo.elide.standalone.config.ElideStandaloneAnalyticSettings;
import com.yahoo.elide.standalone.config.ElideStandaloneAsyncSettings;
import com.yahoo.elide.standalone.config.ElideStandaloneSettings;
import com.yahoo.elide.standalone.config.ElideStandaloneSubscriptionSettings;
import example.models.Post;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

import jakarta.jms.ConnectionFactory;

import java.time.Duration;
import java.util.Properties;

/**
 * Settings class extending ElideStandaloneSettings for tests.
 */
public class ElideStandaloneTestSettings implements ElideStandaloneSettings {

    @Override
    public JsonApiSettingsBuilder getJsonApiSettingsBuilder(EntityDictionary dictionary, JsonApiMapper mapper) {
        String jsonApiBaseUrl = getBaseUrl()
                + getJsonApiPathSpec().replace("/*", "")
                + "/";

        return ElideStandaloneSettings.super.getJsonApiSettingsBuilder(dictionary, mapper)
                .links(links -> links.enabled(true).jsonApiLinks(new DefaultJsonApiLinks(jsonApiBaseUrl)));
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

        options.put("jakarta.persistence.jdbc.driver", "org.h2.Driver");
        options.put("jakarta.persistence.jdbc.url", "jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1;");
        options.put("jakarta.persistence.jdbc.user", "sa");
        options.put("jakarta.persistence.jdbc.password", "");
        return options;
    }

    @Override
    public String getModelPackageName() {
        return Post.class.getPackage().getName();
    }

    @Override
    public boolean enableApiDocs() {
        return true;
    }

    @Override
    public boolean enableGraphQL() {
        return true;
    }

    @Override
    public boolean enableJsonApi() {
        return true;
    }

    @Override
    public boolean verboseErrors() {
        return true;
    }

    @Override
    public ElideStandaloneAsyncSettings getAsyncProperties() {
        ElideStandaloneAsyncSettings asyncProperties = new ElideStandaloneAsyncSettings() {
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
            public Duration getQueryMaxRunTime() {
                return Duration.ofSeconds(1800L);
            }

            @Override
            public Duration getQueryRetentionDuration() {
                return Duration.ofDays(3L);
            }

            @Override
            public boolean enableExport() {
                return false;
            }
        };
        return asyncProperties;
    }

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
        return analyticProperties;
    }

    @Override
    public ElideStandaloneSubscriptionSettings getSubscriptionProperties() {
        return new ElideStandaloneSubscriptionSettings() {
            @Override
            public boolean enabled() {
                return false;
            }

            @Override
            public boolean shouldSendPingOnSubscribe() {
                return true;
            }

            @Override
            public ConnectionFactory getConnectionFactory() {
                return new ActiveMQConnectionFactory("vm://0");
            }
        };
    }
}
