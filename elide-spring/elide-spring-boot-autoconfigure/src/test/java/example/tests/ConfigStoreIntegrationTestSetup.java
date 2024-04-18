/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.tests;

import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.dictionary.Injector;
import com.paiondata.elide.core.security.checks.Check;
import com.paiondata.elide.core.type.Type;
import com.paiondata.elide.core.utils.ClassScanner;
import com.paiondata.elide.core.utils.coerce.CoerceUtil;
import com.paiondata.elide.modelconfig.DynamicConfiguration;
import com.paiondata.elide.modelconfig.store.models.ConfigChecks;
import com.paiondata.elide.spring.config.ElideConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@TestConfiguration
public class ConfigStoreIntegrationTestSetup {

    @Bean
    public EntityDictionary buildDictionary(AutowireCapableBeanFactory beanFactory,
                                            ClassScanner scanner,
                                            @Autowired(required = false) DynamicConfiguration dynamicConfig,
                                            ElideConfigProperties settings,
                                            @Qualifier("entitiesToExclude") Set<Type<?>> entitiesToExclude) {

        Map<String, Class<? extends Check>> checks = new HashMap<>();

        if (settings.getAggregationStore().getDynamicConfig().getConfigApi().isEnabled()) {
            checks.put(ConfigChecks.CAN_CREATE_CONFIG, ConfigChecks.CanCreate.class);
            checks.put(ConfigChecks.CAN_READ_CONFIG, ConfigChecks.CanRead.class);
            checks.put(ConfigChecks.CAN_DELETE_CONFIG, ConfigChecks.CanDelete.class);
            checks.put(ConfigChecks.CAN_UPDATE_CONFIG, ConfigChecks.CanNotUpdate.class);
        }

        EntityDictionary dictionary = new EntityDictionary(
                checks, //Checks
                new HashMap<>(), //Role Checks
                new Injector() {
                    @Override
                    public void inject(Object entity) {
                        beanFactory.autowireBean(entity);
                    }

                    @Override
                    public <T> T instantiate(Class<T> cls) {
                        return beanFactory.createBean(cls);
                    }
                },
                CoerceUtil::lookup, //Serde Lookup
                entitiesToExclude,
                scanner);

        return dictionary;
    }
}
