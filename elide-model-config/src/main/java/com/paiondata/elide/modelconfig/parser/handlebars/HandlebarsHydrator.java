/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.modelconfig.parser.handlebars;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.EscapingStrategy;
import com.github.jknack.handlebars.EscapingStrategy.Hbs;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;

import java.io.IOException;
import java.util.Map;

/**
 * Class for handlebars hydration.
 */
public class HandlebarsHydrator {

    public static final String HANDLEBAR_START_DELIMITER = "<%";
    public static final String HANDLEBAR_END_DELIMITER = "%>";
    public static final EscapingStrategy MY_ESCAPING_STRATEGY = new Hbs(new String[][]{
        {"\"", "&quot;" },
        {"`", "&#x60;" },
        {"\n", " " }
    });

    private final Handlebars handlebars;

    public HandlebarsHydrator() {
        TemplateLoader loader = new ClassPathTemplateLoader("/templates");
        this.handlebars = new Handlebars(loader).with(MY_ESCAPING_STRATEGY);
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
}
