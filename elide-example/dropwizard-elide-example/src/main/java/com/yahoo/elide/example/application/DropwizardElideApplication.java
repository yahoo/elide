/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.example.application;

import com.yahoo.elide.contrib.dropwizard.elide.ElideBundle;
import com.yahoo.elide.example.Author;
import com.yahoo.elide.example.Book;
import com.yahoo.elide.resources.JsonApiEndpoint;

import io.dropwizard.Application;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

/**
 * A Dropwizard Application for Elide.
 */
public class DropwizardElideApplication extends Application<DropwizardElideConfiguration> {
    private final String name = "dropwizard-elide-example";

    private final ElideBundle<DropwizardElideConfiguration> elideBundle;

    public DropwizardElideApplication() {
        this.elideBundle = new ElideBundle<DropwizardElideConfiguration>(
                Author.class,
                Book.class
        ) {
            @Override
            public DataSourceFactory getDataSourceFactory(DropwizardElideConfiguration configuration) {
                return configuration.getDataSourceFactory();
            }
        };
    }

    @Override
    public void initialize(Bootstrap<DropwizardElideConfiguration> bootstrap) {
        bootstrap.addBundle(elideBundle);
    }

    @Override
    public void run(DropwizardElideConfiguration config, Environment environment) {
        environment.jersey().register(JsonApiEndpoint.class);
    }

    public static void main(String[] args) throws Exception {
//        new DropwizardElideApplication().run("server", "elide-example/dropwizard-elide-example/example.yml");
        new DropwizardElideApplication().run(args);
    }

    @Override
    public String getName() {
        return name;
    }
}
