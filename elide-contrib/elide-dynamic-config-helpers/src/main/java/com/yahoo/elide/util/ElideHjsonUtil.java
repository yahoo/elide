/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.util;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.hjson.JsonValue;
import org.json.JSONObject;
import org.json.JSONTokener;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class ElideHjsonUtil {

    public static String hjsonToJson(String hjson) {
        return JsonValue.readHjson(hjson).toString();
    }

    public static String readConfigFile(String filePath) throws Exception {

        BufferedReader reader = null;
        try {
            reader = (filePath.startsWith(ElideDynamicConfigConstants.HTTP_PREFIX)
                            ? getHttpFileReader(filePath) : getLocalFileReader(filePath));
            return readFileContent(reader);
        } catch (Exception e) {
            throw e;
        } finally {
            reader.close();
        }
    }

    public static boolean validateDataWithSchema(String schemaType, String jsonConfig) {

        try {
            JSONObject schemaObj = schemaToJsonObject(schemaType);
            JSONObject data = new JSONObject(new JSONTokener(jsonConfig));

            Schema schema = SchemaLoader.load(schemaObj);
            schema.validate(data);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            return false;
        }
    }

    public static boolean isNull(String input) {
        return (input == null || input.trim().length() == 0) ? true : false;
    }

    private static String readFileContent(BufferedReader reader) throws IOException {
        StringBuffer sb = new StringBuffer();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
            sb.append(ElideDynamicConfigConstants.NEW_LINE);
        }
        return sb.toString();
    }

    private static BufferedReader getHttpFileReader(String filePath) throws Exception {
        URL url = new URL(filePath);
        return new BufferedReader(new InputStreamReader(url.openStream()));
    }

    private static BufferedReader getLocalFileReader(String filePath) throws Exception {
        Path path = Paths.get(filePath);
        return Files.newBufferedReader(path, Charset.forName(ElideDynamicConfigConstants.CHAR_SET));
    }

    private static JSONObject loadSchema(String confFile) throws IOException {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        File file = new File(classLoader.getResource(confFile).getFile());
        String content = new String(Files.readAllBytes(file.toPath()));
        return new JSONObject(new JSONTokener(content));
    }

    private static JSONObject schemaToJsonObject(String schemaType) throws IOException {

        switch (schemaType) {
            case ElideDynamicConfigConstants.SCHEMA_TYPE_TABLE:
                return loadSchema(ElideDynamicConfigConstants.ELIDE_TABLE_VALIDATION_SCHEMA);

            case ElideDynamicConfigConstants.SCHEMA_TYPE_SECURITY:
                return loadSchema(ElideDynamicConfigConstants.ELIDE_SECURITY_SCHEMA);

            case ElideDynamicConfigConstants.SCHEMA_TYPE_VARIABLE:
                return loadSchema(ElideDynamicConfigConstants.ELIDE_VARIABLE_SCHEMA);
            default:
                return null;
        }
    }
}
