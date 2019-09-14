/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.example;

import com.yahoo.elide.standalone.ElideStandalone;
import com.yahoo.elide.standalone.config.ElideStandaloneSettings;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Properties;

/**
 * Example backend using Elide library.
 */
@Slf4j
public class Main {
    public static void main(String[] args) throws Exception {
        ElideStandalone elide = new ElideStandalone(new ElideStandaloneSettings() {
            public int getPort() {
                return 4080;
            }

            public String getJsonApiPathSpec() {
                return "/*";
            }

            public String getModelPackageName() {

                    //This needs to be changed to the package where your models live.
                    return "com.yahoo.elide.example.models";
            }

            public Properties getDatabaseProperties() {
                Properties dbProps;
                try {
                    dbProps = new Properties();
                    dbProps.load(
                            Main.class.getClassLoader().getResourceAsStream("dbconfig.properties")
                    );
                    return dbProps;
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        });

        elide.start();
    }
}
