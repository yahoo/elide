/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.tests;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.audit.Slf4jLogger;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.jsonapi.links.DefaultJSONApiLinks;
import com.yahoo.elide.spring.config.ElideConfigProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.TimeZone;

@TestConfiguration
public class IntegrationTestSetup {

    @Bean
    @Primary
    public Elide initializeElide(EntityDictionary dictionary,
                                 DataStore dataStore, ElideConfigProperties settings) {

        ElideSettingsBuilder builder = new ElideSettingsBuilder(dataStore)
                .withEntityDictionary(dictionary)
                .withDefaultMaxPageSize(settings.getMaxPageSize())
                .withDefaultPageSize(settings.getPageSize())
                .withJSONApiLinks(new DefaultJSONApiLinks())
                .withJoinFilterDialect(new RSQLFilterDialect(dictionary))
                .withSubqueryFilterDialect(new RSQLFilterDialect(dictionary))
                .withAuditLogger(new Slf4jLogger())
                .withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"));

        return new Elide(builder.build());
    }
}
