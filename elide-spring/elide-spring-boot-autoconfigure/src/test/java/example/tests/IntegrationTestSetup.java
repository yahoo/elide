/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.tests;

import static org.mockito.Mockito.spy;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.RefreshableElide;
import com.yahoo.elide.async.AsyncSettings;
import com.yahoo.elide.core.TransactionRegistry;
import com.yahoo.elide.core.audit.Slf4jLogger;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.ErrorMapper;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.graphql.GraphQLSettings;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.jsonapi.JsonApiSettings;
import com.yahoo.elide.jsonapi.links.DefaultJsonApiLinks;
import com.yahoo.elide.spring.config.ElideConfigProperties;
import com.yahoo.elide.utils.HeaderProcessor;
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
                                            ErrorMapper errorMapper) {

        ElideSettings.ElideSettingsBuilder builder = ElideSettings.builder().dataStore(dataStore)
                .entityDictionary(dictionary)
                .errorMapper(errorMapper)
                .objectMapper(mapper.getObjectMapper())
                .defaultMaxPageSize(settings.getMaxPageSize())
                .defaultPageSize(settings.getPageSize())
                .auditLogger(new Slf4jLogger())
                .baseUrl(settings.getBaseUrl())
                .headerProcessor(headerProcessor)
                .serdes(serdes -> serdes.withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC")));

        GraphQLSettings.GraphQLSettingsBuilder graphqlSettings = GraphQLSettings.builder().path(settings.getGraphql().getPath());
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
                .subqueryFilterDialect(RSQLFilterDialect.builder().dictionary(dictionary).build());


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
