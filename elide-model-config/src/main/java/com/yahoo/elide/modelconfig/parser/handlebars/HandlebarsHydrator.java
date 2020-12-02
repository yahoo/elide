/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.parser.handlebars;

import com.yahoo.elide.modelconfig.StaticModelsDetails;
import com.yahoo.elide.modelconfig.model.ElideSecurityConfig;
import com.yahoo.elide.modelconfig.model.ElideTableConfig;
import com.yahoo.elide.modelconfig.model.Table;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.EscapingStrategy;
import com.github.jknack.handlebars.EscapingStrategy.Hbs;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.helper.AssignHelper;
import com.github.jknack.handlebars.helper.ConditionalHelpers;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Class for handlebars hydration.
 */
public class HandlebarsHydrator {

    public static final String SECURITY_CLASS_PREFIX = "DynamicConfigOperationChecks";
    public static final String HANDLEBAR_START_DELIMITER = "<%";
    public static final String HANDLEBAR_END_DELIMITER = "%>";
    public static final EscapingStrategy MY_ESCAPING_STRATEGY = new Hbs(new String[][]{
        {"\"", "&quot;" },
        {"`", "&#x60;" },
        {"\n", " " }
    });

    private final Handlebars handlebars;
    private final HandlebarsHelper helper;

    public HandlebarsHydrator(StaticModelsDetails staticModelDetails) {
        TemplateLoader loader = new ClassPathTemplateLoader("/templates");
        this.helper = new HandlebarsHelper(staticModelDetails);
        this.handlebars = new Handlebars(loader).with(MY_ESCAPING_STRATEGY);
        this.handlebars.registerHelpers(ConditionalHelpers.class);
        this.handlebars.registerHelper(AssignHelper.NAME, AssignHelper.INSTANCE);
        this.handlebars.registerHelpers(helper);
    }

    public HandlebarsHydrator() {
        this.handlebars = new Handlebars();
        this.helper = null;
    }

    /**
     * Method to hydrate the Table template.
     * @param table ElideTable object
     * @return map with key as table java class name and value as table java class definition
     * @throws IOException IOException
     */
    public Map<String, String> hydrateTableTemplate(ElideTableConfig table) throws IOException {

        Map<String, String> tableClasses = new HashMap<>();

        Template template = handlebars.compile("table", HANDLEBAR_START_DELIMITER, HANDLEBAR_END_DELIMITER);
        for (Table t : table.getTables()) {
            tableClasses.put(helper.capitalizeFirstLetter(t.getName()), template.apply(t));
        }

        return tableClasses;
    }

    /**
     * Method to replace variables in hjson config.
     * @param config hjson config string
     * @param replacements Map of variable key value pairs
     * @return hjson config string with variables replaced
     * @throws IOException IOException
     */
    public String hydrateConfigTemplate(String config, Map<String, Object> replacements) throws IOException {

        Context context = Context.newBuilder(replacements).build();
        Template template = handlebars.compileInline(config, HANDLEBAR_START_DELIMITER, HANDLEBAR_END_DELIMITER);

        return template.apply(context);
    }

    /**
     * Method to hydrate the Security template.
     * @param security ElideSecurity Object
     * @return security java class string
     * @throws IOException IOException
     */
    public Map<String, String> hydrateSecurityTemplate(ElideSecurityConfig security) throws IOException {

        Map<String, String> securityClasses = new HashMap<>();

        if (security == null) {
            return securityClasses;
        }

        Template template = handlebars.compile("security", HANDLEBAR_START_DELIMITER, HANDLEBAR_END_DELIMITER);
        for (String role : security.getRoles()) {
            securityClasses.put(SECURITY_CLASS_PREFIX + helper.createSecurityIdenitfier(role, false),
                            template.apply(role));
        }

        return securityClasses;
    }
}
