/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.tests;

import com.yahoo.elide.contrib.dynamicconfighelpers.DBPasswordExtractor;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.DBConfig;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

@TestConfiguration
public class DBPasswordExtractorSetup {

    @Bean
    public DBPasswordExtractor getDBPasswordExtractor() {

        return new DBPasswordExtractor() {
            @Override
            public String getDBPassword(DBConfig config) {
                String encrypted = (String) config.getPropertyMap().get("encrypted.password");
                byte[] decrypted = Base64.getDecoder().decode(encrypted.getBytes());
                try {
                    return new String(decrypted, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
    }
}
