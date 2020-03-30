/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers;

import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideSecurity;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideTable;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.hjson.JsonValue;
import org.json.JSONObject;
import org.json.JSONTokener;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Slf4j
public class Util {

    public static final String SCHEMA_TYPE_TABLE = "table";
    public static final String SCHEMA_TYPE_SECURITY = "security";
    public static final String SCHEMA_TYPE_VARIABLE = "variable";

    public static final String ELIDE_TABLE_VALIDATION_SCHEMA = "elideTableSchema.json";
    public static final String ELIDE_SECURITY_SCHEMA = "elideSecuritySchema.json";
    public static final String ELIDE_VARIABLE_SCHEMA = "elideVariableSchema.json";

    public static final String INVALID_ERROR_MSG = "Incompatible or invalid config";
    public static final String HTTP_PREFIX = "http";
    public static final String CHAR_SET = "UTF-8";
    public static final String NEW_LINE = "\n";

    public static String hjsonToJson(String hjson) {
        return JsonValue.readHjson(hjson).toString();
    }

    public static String readConfigFile(String filePath) throws Exception {
        try {
            BufferedReader reader = (filePath.startsWith(HTTP_PREFIX)
                            ? getHttpFileReader(filePath) : getLocalFileReader(filePath));
            return readFileContent(reader);
        } catch (Exception e) {
            throw e;
        }
    }

    public static boolean validateDataWithSchema(JSONObject schemaObj, String jsonConfig) {

        try {
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

    public static boolean isNullOrEmpty(String input) {
        return (input == null || input.trim().length() == 0);
    }

    private static String readFileContent(BufferedReader reader) throws IOException {
        try {
            StringBuffer sb = new StringBuffer();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append(NEW_LINE);
            }
            return sb.toString();
        } finally {
            reader.close();
        }
    }

    private static BufferedReader getHttpFileReader(String filePath) throws Exception {
        URL url = new URL(filePath);
        return new BufferedReader(new InputStreamReader(url.openStream()));
    }

    private static BufferedReader getLocalFileReader(String filePath) throws Exception {
        Path path = Paths.get(filePath);
        return Files.newBufferedReader(path, Charset.forName(CHAR_SET));
    }

    private static JSONObject loadSchema(String confFilePath) throws IOException {
        try (InputStream stream = ClassLoader.getSystemClassLoader().getResourceAsStream(confFilePath);
                        InputStreamReader reader = new InputStreamReader(stream)) {
            String content = readFileContent(new BufferedReader(reader));
            return new JSONObject(new JSONTokener(content));
        }
    }

    public static JSONObject schemaToJsonObject(String schemaType) throws IOException {

        switch (schemaType) {
            case SCHEMA_TYPE_TABLE:
                return loadSchema(ELIDE_TABLE_VALIDATION_SCHEMA);

            case SCHEMA_TYPE_SECURITY:
                return loadSchema(ELIDE_SECURITY_SCHEMA);

            case SCHEMA_TYPE_VARIABLE:
                return loadSchema(ELIDE_VARIABLE_SCHEMA);
            default:
                return null;
        }
    }

    public static Object getModelPojo(String inputConfigType, String jsonConfig) throws Exception {

        switch (inputConfigType) {
            case Util.SCHEMA_TYPE_TABLE:
                return new ObjectMapper().readValue(jsonConfig, ElideTable.class);

            case Util.SCHEMA_TYPE_SECURITY:
                return new ObjectMapper().readValue(jsonConfig, ElideSecurity.class);

            case Util.SCHEMA_TYPE_VARIABLE:
                return new ObjectMapper().readValue(jsonConfig, Map.class);

            default:
                return null;
        }
    }
}
