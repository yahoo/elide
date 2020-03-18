/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.parser.handlebars;

import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideTable;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.Table;

import com.github.jknack.handlebars.EscapingStrategy;
import com.github.jknack.handlebars.EscapingStrategy.Hbs;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.helper.ConditionalHelpers;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;

import java.io.IOException;

public class TableHandlebar {

    public static final EscapingStrategy MY_ESCAPING_STRATEGY = new Hbs(new String[][]{
        {"<", "&lt;" },
        {">", "&gt;" },
        {"\"", "&quot;" },
        {"`", "&#x60;" },
        {"&", "&amp;" }
    });

    public String hydrateTableTemplate(ElideTable table) throws IOException {

        String tableClassAsString = null;

        TemplateLoader loader = new ClassPathTemplateLoader("/handlebars");
        Handlebars handlebars = new Handlebars(loader).with(MY_ESCAPING_STRATEGY);
        handlebars.registerHelpers(ConditionalHelpers.class);
        handlebars.registerHelpers(new HelperSource());
        Template template = handlebars.compile("table");

        for (Table t : table.getTables()) {
            tableClassAsString = template.apply(t);
        }

        return tableClassAsString;
    }
}
