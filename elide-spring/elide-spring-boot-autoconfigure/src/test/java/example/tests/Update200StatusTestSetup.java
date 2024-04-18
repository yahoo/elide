/*
 * Copyright 2022, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.tests;

import com.paiondata.elide.Elide;
import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.RefreshableElide;
import com.paiondata.elide.core.TransactionRegistry;
import com.paiondata.elide.core.audit.Slf4jLogger;
import com.paiondata.elide.core.datastore.DataStore;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.exceptions.ExceptionLogger;
import com.paiondata.elide.core.exceptions.ExceptionMappers;
import com.paiondata.elide.core.exceptions.ExceptionMappers.ExceptionMappersBuilder;
import com.paiondata.elide.core.exceptions.Slf4jExceptionLogger;
import com.paiondata.elide.core.filter.dialect.RSQLFilterDialect;
import com.paiondata.elide.graphql.DefaultGraphQLErrorMapper;
import com.paiondata.elide.graphql.DefaultGraphQLExceptionHandler;
import com.paiondata.elide.graphql.GraphQLSettings;
import com.paiondata.elide.jsonapi.DefaultJsonApiErrorMapper;
import com.paiondata.elide.jsonapi.DefaultJsonApiExceptionHandler;
import com.paiondata.elide.jsonapi.JsonApiMapper;
import com.paiondata.elide.jsonapi.JsonApiSettings;
import com.paiondata.elide.spring.config.ElideConfigProperties;
import com.paiondata.elide.utils.HeaderProcessor;
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
                                                ExceptionMappersBuilder exceptionMappersBuilder) {
        ExceptionMappers exceptionMappers = exceptionMappersBuilder.build();
        ExceptionLogger exceptionLogger = new Slf4jExceptionLogger();
        JsonApiSettings.JsonApiSettingsBuilder jsonApiSettings = JsonApiSettings.builder()
                .path(settings.getJsonApi().getPath())
                .joinFilterDialect(RSQLFilterDialect.builder().dictionary(dictionary).build())
                .subqueryFilterDialect(RSQLFilterDialect.builder().dictionary(dictionary).build())
                .jsonApiMapper(mapper)
                .updateStatus200()
                .jsonApiExceptionHandler(new DefaultJsonApiExceptionHandler(exceptionLogger, exceptionMappers,
                        new DefaultJsonApiErrorMapper()));
        GraphQLSettings.GraphQLSettingsBuilder graphqlSettings = GraphQLSettings.builder().path(settings.getGraphql().getPath())
                .graphqlExceptionHandler(new DefaultGraphQLExceptionHandler(exceptionLogger, exceptionMappers,
                        new DefaultGraphQLErrorMapper()));
        ElideSettings.ElideSettingsBuilder builder = ElideSettings.builder().dataStore(dataStore)
                .entityDictionary(dictionary)
                .objectMapper(mapper.getObjectMapper())
                .maxPageSize(settings.getMaxPageSize())
                .defaultPageSize(settings.getDefaultPageSize())
                .auditLogger(new Slf4jLogger())
                .baseUrl(settings.getBaseUrl())
                .serdes(serdes -> serdes.withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC")))
                .headerProcessor(headerProcessor)
                .settings(graphqlSettings, jsonApiSettings);

        Elide elide = new Elide(builder.build(), transactionRegistry, dictionary.getScanner(), true);

        return new RefreshableElide(elide);
    }
}
