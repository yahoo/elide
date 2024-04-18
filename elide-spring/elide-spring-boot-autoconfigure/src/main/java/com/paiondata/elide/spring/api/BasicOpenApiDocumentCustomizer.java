/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.api;

import com.paiondata.elide.swagger.OpenApiDocument;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotatedElementUtils;

import io.swagger.v3.core.util.AnnotationsUtils;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Basic customization of the OpenAPI document.
 */
public class BasicOpenApiDocumentCustomizer implements OpenApiDocumentCustomizer, ApplicationContextAware {
    private ApplicationContext applicationContext;

    public void customize(OpenAPI openApi) {
        Components components = openApi.getComponents();
        if (components == null) {
            components = new Components();
            openApi.components(components);
        }
        getSecuritySchemes().forEach(components::addSecuritySchemes);

        OpenAPIDefinition openApiDefinition = getOpenApiDefinition();
        if (openApiDefinition != null) {
            applyDefinition(openApi, openApiDefinition);
        }

        if (openApi.getInfo() == null) {
            openApi.info(new Info().title(OpenApiDocument.DEFAULT_TITLE));
        } else if (openApi.getInfo().getTitle() == null || openApi.getInfo().getTitle().isBlank()) {
            openApi.getInfo().setTitle(OpenApiDocument.DEFAULT_TITLE);
        }

        sort(openApi);
    }

    /**
     * Sorts the OpenAPI document.
     *
     * @param openApi the document to sort
     */
    protected void sort(OpenAPI openApi) {
        List<Tag> tags = openApi.getTags();
        if (tags != null) {
            tags.sort((left, right) -> left.getName().compareTo(right.getName()));
        }
    }

    protected Map<String, io.swagger.v3.oas.models.security.SecurityScheme> getSecuritySchemes() {
        Map<String, io.swagger.v3.oas.models.security.SecurityScheme> securitySchemes = new HashMap<>();
        Map<String, Object> beans = this.applicationContext.getBeansWithAnnotation(SecurityScheme.class);
        for (Object bean : beans.values()) {
            SecurityScheme annotation = AnnotatedElementUtils.findMergedAnnotation(bean.getClass(),
                    SecurityScheme.class);
            if (annotation != null) {
                io.swagger.v3.oas.models.security.SecurityScheme model =
                        new io.swagger.v3.oas.models.security.SecurityScheme();
                model.setIn(getIn(annotation.in()));
                model.setType(getType(annotation.type()));
                model.setBearerFormat(annotation.bearerFormat());
                model.setScheme(annotation.scheme());
                model.setOpenIdConnectUrl(annotation.openIdConnectUrl());
                if (annotation.ref() != null && !annotation.ref().isBlank()) {
                    model.set$ref(annotation.ref());
                }
                model.setName(annotation.name());
                securitySchemes.put(annotation.name(), model);
            }
        }
        return securitySchemes;
    }

    protected OpenAPIDefinition getOpenApiDefinition() {
        Map<String, Object> beans = this.applicationContext.getBeansWithAnnotation(OpenAPIDefinition.class);
        if (!beans.isEmpty()) {
            Object bean = beans.values().iterator().next();
            return AnnotatedElementUtils.findMergedAnnotation(bean.getClass(),
                    OpenAPIDefinition.class);
        }
        return null;
    }

    public static void applyDefinition(OpenAPI openApi, OpenAPIDefinition openApiDefinition) {
        AnnotationsUtils.getInfo(openApiDefinition.info()).ifPresent(openApi::setInfo);
        AnnotationsUtils.getExternalDocumentation(openApiDefinition.externalDocs()).ifPresent(openApi::setExternalDocs);
        AnnotationsUtils.getTags(openApiDefinition.tags(), false).ifPresent(tags -> tags.forEach(openApi::addTagsItem));

        io.swagger.v3.oas.models.security.SecurityRequirement model =
                new io.swagger.v3.oas.models.security.SecurityRequirement();

        for (SecurityRequirement securityRequirement : openApiDefinition.security()) {
            model.addList(securityRequirement.name());
        }

        openApi.addSecurityItem(model);
    }

    protected io.swagger.v3.oas.models.security.SecurityScheme.In getIn(SecuritySchemeIn value) {
        if (value == null) {
            return null;
        }
        return Arrays.stream(io.swagger.v3.oas.models.security.SecurityScheme.In.values())
                .filter(in -> in.toString().equals(value.toString())).findFirst().orElse(null);
    }

    protected io.swagger.v3.oas.models.security.SecurityScheme.Type getType(SecuritySchemeType value) {
        if (value == null) {
            return null;
        }
        return Arrays.stream(io.swagger.v3.oas.models.security.SecurityScheme.Type.values())
                .filter(type -> type.toString().equals(value.toString())).findFirst().orElse(null);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
