/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.tests;

import com.yahoo.elide.core.security.RequestScope;
import com.yahoo.elide.core.security.checks.OperationCheck;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.Optional;
import javax.inject.Named;

@TestConfiguration
public class ConfigStoreIntegrationTestSetup {

    @Bean
    @Named("ConfigReadCheck")
    OperationCheck getReadCheck() {
        return new OperationCheck() {
            @Override
            public boolean ok(Object object, RequestScope requestScope, Optional optional) {
                return true;
            }
        };
    }

    @Bean
    @Named("ConfigCreateCheck")
    OperationCheck getCreateCheck() {
        return new OperationCheck() {
            @Override
            public boolean ok(Object object, RequestScope requestScope, Optional optional) {
                return true;
            }
        };
    }

    @Bean
    @Named("ConfigDeleteCheck")
    OperationCheck getDeleteCheck() {
        return new OperationCheck() {
            @Override
            public boolean ok(Object object, RequestScope requestScope, Optional optional) {
                return true;
            }
        };
    }

    @Bean
    @Named("ConfigUpdateCheck")
    OperationCheck getUpdateCheck() {
        return new OperationCheck() {
            @Override
            public boolean ok(Object object, RequestScope requestScope, Optional optional) {
                return false;
            }
        };
    }
}
