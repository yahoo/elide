/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.tests;

import static org.mockito.Mockito.spy;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.RefreshableElide;
import com.yahoo.elide.core.TransactionRegistry;
import com.yahoo.elide.core.audit.Slf4jLogger;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.ErrorMapper;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.jsonapi.links.DefaultJSONApiLinks;
import com.yahoo.elide.spring.config.ElideConfigProperties;
import com.yahoo.elide.utils.HeaderUtils;
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
                                            HeaderUtils.HeaderProcessor headerProcessor,
                                            JsonApiMapper mapper,
                                            ErrorMapper errorMapper) {

        ElideSettingsBuilder builder = new ElideSettingsBuilder(dataStore)
                .withEntityDictionary(dictionary)
                .withErrorMapper(errorMapper)
                .withJsonApiMapper(mapper)
                .withDefaultMaxPageSize(settings.getMaxPageSize())
                .withDefaultPageSize(settings.getPageSize())
                .withJoinFilterDialect(RSQLFilterDialect.builder().dictionary(dictionary).build())
                .withSubqueryFilterDialect(RSQLFilterDialect.builder().dictionary(dictionary).build())
                .withAuditLogger(new Slf4jLogger())
                .withBaseUrl(settings.getBaseUrl())
                .withHeaderProcessor(headerProcessor)
                .withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"))
                .withJsonApiPath(settings.getJsonApi().getPath())
                .withGraphQLApiPath(settings.getGraphql().getPath());

        if (settings.isVerboseErrors()) {
            builder.withVerboseErrors();
        }

        if (settings.getAsync() != null
                && settings.getAsync().getExport() != null
                && settings.getAsync().getExport().isEnabled()) {
            builder.withExportApiPath(settings.getAsync().getExport().getPath());
        }

        if (settings.getJsonApi() != null
                && settings.getJsonApi().isEnabled()
                && settings.getJsonApi().isEnableLinks()) {
            String baseUrl = settings.getBaseUrl();

            if (StringUtils.isEmpty(baseUrl)) {
                builder.withJSONApiLinks(new DefaultJSONApiLinks());
            } else {
                String jsonApiBaseUrl = baseUrl + settings.getJsonApi().getPath() + "/";
                builder.withJSONApiLinks(new DefaultJSONApiLinks(jsonApiBaseUrl));
            }
        }

        Elide elide = new Elide(builder.build(), new TransactionRegistry(), dictionary.getScanner(), true);

        return new RefreshableElide(spy(elide));
    }
}
