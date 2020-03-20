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
import java.util.ArrayList;
import java.util.List;

/**
 * Class for handlebars hydration.
 */
public class HandlebarsHydrator {

    public static final EscapingStrategy MY_ESCAPING_STRATEGY = new Hbs(new String[][]{
        {"<", "&lt;" },
        {">", "&gt;" },
        {"\"", "&quot;" },
        {"`", "&#x60;" },
        {"&", "&amp;" }
    });

    /**
     * Method to hydrate the Table template.
     * @param table
     * @return table java class list
     * @throws IOException
     */
    public List<String> hydrateTableTemplate(ElideTable table) throws IOException {

        List<String> tableClassStringList = new ArrayList<>();

        TemplateLoader loader = new ClassPathTemplateLoader("/templates");
        Handlebars handlebars = new Handlebars(loader).with(MY_ESCAPING_STRATEGY);
        handlebars.registerHelpers(ConditionalHelpers.class);
        handlebars.registerHelpers(new HandlebarsHelper());
        Template template = handlebars.compile("table");

        for (Table t : table.getTables()) {
            tableClassStringList.add(template.apply(t));
        }

        return tableClassStringList;
    }
    
    /**
     * Method to return the List of Class Names hydrated.
     * @param table
     * @return table java class name list
     * @throws IOException
     */
    public List<String> getTableClassNames(ElideTable table) throws IOException {

        List<String> tableClassStringNameList = new ArrayList<>();

        HandlebarsHelper helper = new HandlebarsHelper();
        
        for (Table t : table.getTables()) {
        	tableClassStringNameList.add(helper.capitalizeFirstLetter(t.getName()));
        }

        return tableClassStringNameList;
    }
}
