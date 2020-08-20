/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.example;

import com.yahoo.elide.contrib.swagger.SwaggerBuilder;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.example.models.Comment;
import com.yahoo.elide.example.models.Post;
import com.yahoo.elide.example.models.User;
import com.yahoo.elide.standalone.config.ElideStandaloneSettings;
import io.swagger.models.Info;
import io.swagger.models.Swagger;

import java.util.HashMap;
import java.util.Map;
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
    public Map<String, Swagger> enableSwagger() {
        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());

        dictionary.bindEntity(User.class);
        dictionary.bindEntity(Post.class);
        dictionary.bindEntity(Comment.class);
        Info info = new Info().title("Test Service").version("1.0");

        SwaggerBuilder builder = new SwaggerBuilder(dictionary, info).withLegacyFilterDialect(false);
        Swagger swagger = builder.build();

        Map<String, Swagger> docs = new HashMap<>();
        docs.put("test", swagger);
        return docs;
    }

    @Override
    public String getModelPackageName() {

        //This needs to be changed to the package where your models live.
        return "com.yahoo.elide.example.models";
    }
}
