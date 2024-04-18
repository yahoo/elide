/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.tests;

import static org.mockito.Mockito.spy;

import com.paiondata.elide.Elide;
import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.RefreshableElide;
import com.paiondata.elide.async.AsyncSettings;
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
import com.paiondata.elide.jsonapi.links.DefaultJsonApiLinks;
import com.paiondata.elide.spring.config.ElideConfigProperties;
import com.paiondata.elide.utils.HeaderProcessor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.TimeZone;

@TestConfiguration
public class IntegrationTestSetup {
    //Initialize beans here if needed.

    //We recreate the Elide bean here without @RefreshScope so that it can be used With @SpyBean.
    @Bean
    public RefreshableElide initializeElide(EntityDictionary dictionary,
                                            DataStore dataStore,
                                            ElideConfigProperties settings,
                                            HeaderProcessor headerProcessor,
                                            JsonApiMapper mapper,
                                            ExceptionMappersBuilder exceptionMappersBuilder) {

        ElideSettings.ElideSettingsBuilder builder = ElideSettings.builder().dataStore(dataStore)
                .entityDictionary(dictionary)
                .objectMapper(mapper.getObjectMapper())
                .maxPageSize(settings.getMaxPageSize())
                .defaultPageSize(settings.getDefaultPageSize())
                .auditLogger(new Slf4jLogger())
                .baseUrl(settings.getBaseUrl())
                .headerProcessor(headerProcessor)
                .serdes(serdes -> serdes.withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC")));

        ExceptionLogger exceptionLogger = new Slf4jExceptionLogger();
        ExceptionMappers exceptionMappers = exceptionMappersBuilder.build();

        GraphQLSettings.GraphQLSettingsBuilder graphqlSettings = GraphQLSettings.builder()
                .path(settings.getGraphql().getPath()).graphqlExceptionHandler(new DefaultGraphQLExceptionHandler(
                        exceptionLogger, exceptionMappers, new DefaultGraphQLErrorMapper()));
        builder.settings(graphqlSettings);

        if (settings.isVerboseErrors()) {
            builder.verboseErrors(true);
        }

        if (settings.getAsync() != null
                && settings.getAsync().getExport() != null
                && settings.getAsync().getExport().isEnabled()) {
            AsyncSettings.AsyncSettingsBuilder asyncSettings = AsyncSettings.builder()
                    .export(export -> export.path(settings.getAsync().getExport().getPath()));
            builder.settings(asyncSettings);
        }

        JsonApiSettings.JsonApiSettingsBuilder jsonApiSettingsBuilder = JsonApiSettings.builder()
                .path(settings.getJsonApi().getPath())
                .jsonApiMapper(mapper)
                .joinFilterDialect(RSQLFilterDialect.builder().dictionary(dictionary).build())
                .subqueryFilterDialect(RSQLFilterDialect.builder().dictionary(dictionary).build())
                .jsonApiExceptionHandler(new DefaultJsonApiExceptionHandler(exceptionLogger, exceptionMappers,
                        new DefaultJsonApiErrorMapper()));

        if (settings.getJsonApi() != null
                && settings.getJsonApi().isEnabled()
                && settings.getJsonApi().getLinks().isEnabled()) {
            String baseUrl = settings.getBaseUrl();
            jsonApiSettingsBuilder.links(links -> links.enabled(true));
            if (StringUtils.isEmpty(baseUrl)) {
                jsonApiSettingsBuilder.links(links -> links.jsonApiLinks(new DefaultJsonApiLinks()));
            } else {
                String jsonApiBaseUrl = baseUrl + settings.getJsonApi().getPath() + "/";
                jsonApiSettingsBuilder.links(links -> links.jsonApiLinks(new DefaultJsonApiLinks(jsonApiBaseUrl)));
            }
        }

        builder.settings(jsonApiSettingsBuilder);

        Elide elide = new Elide(builder.build(), new TransactionRegistry(), dictionary.getScanner(), true);

        return new RefreshableElide(spy(elide));
    }
}
