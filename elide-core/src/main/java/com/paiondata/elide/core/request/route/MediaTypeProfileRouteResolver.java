/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.request.route;

import static com.paiondata.elide.core.dictionary.EntityDictionary.NO_VERSION;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Resolves a route using a media type profile.
 *
 * <p>This assumes that the profile is in the last path segment.
 *
 * <p>For example: application/vnd.api+json; profile="https://example.org/1.0 https://example.org/profile"
 */
public class MediaTypeProfileRouteResolver extends MediaTypeParameterRouteResolver {
    private static final String MEDIA_TYPE_PARAMETER_PROFILE = "profile=";
    private final String versionPrefix;
    private final UriPrefixSupplier uriPrefixSupplier;
    private final ApiVersionValidator apiVersionValidator;

    public MediaTypeProfileRouteResolver(String versionPrefix, ApiVersionValidator apiVersionValidator,
            UriPrefixSupplier uriPrefixSupplier) {
        super(null);
        this.versionPrefix = versionPrefix;
        this.uriPrefixSupplier = uriPrefixSupplier;
        this.apiVersionValidator = apiVersionValidator;
    }

    @Override
    protected String fromMediaTypeParameter(String parameter) {
        if (parameter.startsWith(MEDIA_TYPE_PARAMETER_PROFILE)) {
            Profile profile = parseProfile(parameter.substring(MEDIA_TYPE_PARAMETER_PROFILE.length()));
            return processProfile(profile);
        }
        return NO_VERSION;
    }

    public String processProfile(Profile profile) {
        String uriPrefix = this.uriPrefixSupplier.getUriPrefix();
        for (String value : profile.getValues()) {
            if (value.startsWith(uriPrefix)) {
                int start = value.lastIndexOf("/");
                if (start != -1 && value.length() > start + 1) {
                    String version = value.substring(start + 1);
                    if (version.startsWith(this.versionPrefix)) {
                        version = version.substring(this.versionPrefix.length());
                        if (apiVersionValidator.isValidApiVersion(version)) {
                            return version;
                        }
                    }
                }
            }
        }
        return NO_VERSION;
    }

    /**
     * The media type profile values.
     */
    public static final class Profile {
        private List<String> values;

        public Profile(List<String> values) {
            this.values = values;
        }

        public List<String> getValues() {
            return this.values;
        }
    }

    /**
     * Parses a space delimited profile parameter into a list.
     *
     * <p>"https://example.com/resource-timestamps https://example.com/api/"
     *
     * @param parameter the profile parameter value
     * @return the profile list
     */
    public Profile parseProfile(String parameter) {
        if (parameter.charAt(0) == '"' && parameter.charAt(parameter.length() - 1) == '"') {
            String list = parameter.substring(1, parameter.length() - 1);
            String[] items = list.split(" ");
            List<String> values = new ArrayList<>(items.length);
            Collections.addAll(values, items);
            return new Profile(values);
        } else {
            return new Profile(Collections.singletonList(parameter));
        }
    }
}
