/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.example;

import com.yahoo.elide.standalone.config.ElideStandaloneSettings;

import java.util.Optional;

/**
 * This class contains common settings for both test and production.
 */
public abstract class CommonElideSettings implements ElideStandaloneSettings {

    @Override
    public int getPort() {
        //Heroku exports port to come from $PORT
        return Optional.ofNullable(System.getenv("PORT"))
                .map(Integer::valueOf)
                .orElse(4080);
    }

    @Override
    public boolean enableSwagger() {
        return true;
    }

    @Override
    public String getModelPackageName() {

        //This needs to be changed to the package where your models live.
        return "com.yahoo.elide.example.models";
    }
}
