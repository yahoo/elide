package com.yahoo.elide.extension.runtime;

import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

import javax.enterprise.context.ApplicationScoped;

@ConfigRoot(name = "elide", phase = ConfigPhase.BUILD_TIME)
public class ElideConfig {
    /*
                    .withEntityDictionary(dictionary)
                .withDefaultMaxPageSize(10000)
                .withDefaultPageSize(100)
                .withJoinFilterDialect(new RSQLFilterDialect(dictionary))
                .withSubqueryFilterDialect(new RSQLFilterDialect(dictionary))
                .withAuditLogger(new Slf4jLogger())
                .withBaseUrl("/")
                .withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"))
                .withJsonApiPath("json")
                .withGraphQLApiPath("graphql");
     */

    /**
     * Foo
     */
    @ConfigItem(defaultValue = "100")
    public int defaultPageSize;

    /**
     * Bar
     */
    @ConfigItem(defaultValue = "10000")
    public int defaultMaxPageSize;
}
