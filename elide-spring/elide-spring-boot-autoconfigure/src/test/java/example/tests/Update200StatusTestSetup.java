/*
 * Copyright 2022, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.tests;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.RefreshableElide;
import com.yahoo.elide.core.TransactionRegistry;
import com.yahoo.elide.core.audit.Slf4jLogger;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.ErrorMapper;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.graphql.GraphQLSettings;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.jsonapi.JsonApiSettings;
import com.yahoo.elide.spring.config.ElideConfigProperties;
import com.yahoo.elide.utils.HeaderProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.TimeZone;

@TestConfiguration
public class Update200StatusTestSetup {
    @Bean
    public RefreshableElide getRefreshableElide(EntityDictionary dictionary,
                                                DataStore dataStore,
                                                HeaderProcessor headerProcessor,
                                                TransactionRegistry transactionRegistry,
                                                ElideConfigProperties settings,
                                                JsonApiMapper mapper,
                                                ErrorMapper errorMapper) {

        JsonApiSettings.JsonApiSettingsBuilder jsonApiSettings = JsonApiSettings.builder()
                .path(settings.getJsonApi().getPath())
                .joinFilterDialect(RSQLFilterDialect.builder().dictionary(dictionary).build())
                .subqueryFilterDialect(RSQLFilterDialect.builder().dictionary(dictionary).build())
                .jsonApiMapper(mapper)
                .updateStatus200();
        GraphQLSettings.GraphQLSettingsBuilder graphqlSettings = GraphQLSettings.builder().path(settings.getGraphql().getPath());

        ElideSettings.ElideSettingsBuilder builder = ElideSettings.builder().dataStore(dataStore)
                .entityDictionary(dictionary)
                .errorMapper(errorMapper)
                .objectMapper(mapper.getObjectMapper())
                .defaultMaxPageSize(settings.getMaxPageSize())
                .defaultPageSize(settings.getPageSize())
                .auditLogger(new Slf4jLogger())
                .baseUrl(settings.getBaseUrl())
                .serdes(serdes -> serdes.withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC")))
                .headerProcessor(headerProcessor)
                .settings(graphqlSettings, jsonApiSettings);

        Elide elide = new Elide(builder.build(), transactionRegistry, dictionary.getScanner(), true);

        return new RefreshableElide(elide);
    }
}
