/*
 * Copyright 2022, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.tests;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.RefreshableElide;
import com.yahoo.elide.core.TransactionRegistry;
import com.yahoo.elide.core.audit.Slf4jLogger;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.dictionary.Injector;
import com.yahoo.elide.core.exceptions.ErrorMapper;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.security.checks.Check;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.core.utils.ClassScanner;
import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.modelconfig.DynamicConfiguration;
import com.yahoo.elide.modelconfig.store.models.ConfigChecks;
import com.yahoo.elide.spring.config.ElideConfigProperties;
import com.yahoo.elide.utils.HeaderUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

@TestConfiguration
public class Update200StatusTestSetup {
    @Bean
    public RefreshableElide getRefreshableElide(EntityDictionary dictionary,
                                                DataStore dataStore,
                                                HeaderUtils.HeaderProcessor headerProcessor,
                                                TransactionRegistry transactionRegistry,
                                                ElideConfigProperties settings,
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
                .withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"))
                .withJsonApiPath(settings.getJsonApi().getPath())
                .withHeaderProcessor(headerProcessor)
                .withGraphQLApiPath(settings.getGraphql().getPath())
                .withUpdate200Status();

        Elide elide = new Elide(builder.build(), transactionRegistry, dictionary.getScanner(), true);

        return new RefreshableElide(elide);
    }
}
