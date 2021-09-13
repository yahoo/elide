/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.extension.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.LifeCycleHookBinding;
import com.yahoo.elide.annotation.SecurityCheck;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.exceptions.ErrorObjects;
import com.yahoo.elide.core.utils.ClassScanner;
import com.yahoo.elide.core.utils.coerce.converters.ElideTypeConverter;
import com.yahoo.elide.extension.runtime.ElideConfig;
import com.yahoo.elide.extension.runtime.ElideRecorder;
import com.yahoo.elide.extension.runtime.ElideResourceBuilder;
import com.yahoo.elide.graphql.GraphQLEndpoint;
import com.yahoo.elide.jsonapi.models.Data;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.jsonapi.models.Meta;
import com.yahoo.elide.jsonapi.models.Resource;
import com.yahoo.elide.jsonapi.resources.JsonApiEndpoint;
import com.yahoo.elide.jsonapi.serialization.DataSerializer;
import com.yahoo.elide.swagger.resources.DocEndpoint;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.resteasy.spi.ResteasyDeployment;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.hibernate.orm.deployment.JpaModelIndexBuildItem;
import io.quarkus.resteasy.server.common.deployment.ResteasyDeploymentCustomizerBuildItem;
import io.quarkus.undertow.deployment.ServletInitParamBuildItem;
import io.swagger.models.ExternalDocs;
import io.swagger.models.Info;
import io.swagger.models.Model;
import io.swagger.models.Response;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import io.swagger.models.auth.SecuritySchemeDefinition;
import io.swagger.models.parameters.Parameter;
import io.swagger.util.Json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import javax.enterprise.inject.Default;
import javax.inject.Singleton;

class ElideExtensionProcessor {

    private static final String FEATURE = "elide-extension";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    public AdditionalBeanBuildItem configureElideEndpoints(ElideConfig config) {
        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder();

        if (config.baseJsonapi != null) {
            System.out.println("Adding JSON-API");
            builder = builder.addBeanClass(JsonApiEndpoint.class);
        }

        if (config.baseGraphql != null) {
            System.out.println("Adding GraphQL API");
            builder = builder.addBeanClass(GraphQLEndpoint.class);
        }

        if (config.baseSwagger != null && config.baseJsonapi != null) {
            System.out.println("Adding Swagger API");
            builder = builder.addBeanClass(DocEndpoint.class);
        }

        return builder.build();
    }

    @BuildStep
    public void configureRestEasy(
            BuildProducer<ResteasyDeploymentCustomizerBuildItem> deploymentCustomizerProducer,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ServletInitParamBuildItem> initParamProducer
    ) {
        initParamProducer.produce(
                new ServletInitParamBuildItem(
                        ResteasyContextParameters.RESTEASY_SCANNED_RESOURCE_CLASSES_WITH_BUILDER,
                        ElideResourceBuilder.class.getName() + ":"
                                + JsonApiEndpoint.class.getCanonicalName() + ","
                                + GraphQLEndpoint.class.getCanonicalName() + ","
                                + DocEndpoint.class.getCanonicalName()
                ));

        deploymentCustomizerProducer.produce(new ResteasyDeploymentCustomizerBuildItem(new Consumer<ResteasyDeployment>() {
            @Override
            public void accept(ResteasyDeployment resteasyDeployment) {
                resteasyDeployment.getScannedResourceClassesWithBuilder().put(
                        ElideResourceBuilder.class.getCanonicalName(),
                        Arrays.asList(
                                JsonApiEndpoint.class.getCanonicalName(),
                                GraphQLEndpoint.class.getCanonicalName(),
                                DocEndpoint.class.getCanonicalName()));
            }
        }));

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, false, ElideResourceBuilder.class.getName()));
    }

    @Record(STATIC_INIT)
    @BuildStep
    void configureElideConfig(ElideConfig elideConfig,
               ElideRecorder recorder,
               BuildProducer<BeanContainerListenerBuildItem> containerListenerProducer) {

        containerListenerProducer.produce(
                new BeanContainerListenerBuildItem(recorder.setElideConfig(elideConfig)));
    }

    @BuildStep
    List<BeanDefiningAnnotationBuildItem> configureBeanAnnotations() {
        List<BeanDefiningAnnotationBuildItem> additionalBeanAnnotations = new ArrayList<>();
        additionalBeanAnnotations.add(
                new BeanDefiningAnnotationBuildItem(DotName.createSimple(Include.class.getCanonicalName())));
        additionalBeanAnnotations.add(
                new BeanDefiningAnnotationBuildItem(DotName.createSimple(SecurityCheck.class.getCanonicalName())));
        additionalBeanAnnotations.add(
                new BeanDefiningAnnotationBuildItem(DotName.createSimple(LifeCycleHookBinding.class.getCanonicalName())));
        additionalBeanAnnotations.add(
                new BeanDefiningAnnotationBuildItem(DotName.createSimple(ElideTypeConverter.class.getCanonicalName())));

        return additionalBeanAnnotations;
    }

    @BuildStep
    @Record(STATIC_INIT)
    public void configureElideModels(
            JpaModelIndexBuildItem index,
            ElideRecorder elideRecorder,
            BuildProducer<ReflectiveClassBuildItem> reflectionBuildItems,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectionHierarchiesBuildItems,
            BuildProducer<SyntheticBeanBuildItem> synthenticBean
    ) {
        List<Class<?>> elideClasses = new ArrayList<>();

        index.getIndex().getKnownClasses().forEach(classInfo -> {
            boolean found = false;

            for (Class annotationClass : ElideRecorder.ANNOTATIONS) {
                AnnotationInstance instance =
                        classInfo.classAnnotation(DotName.createSimple(annotationClass.getCanonicalName()));

                if (instance != null) {
                    found = true;
                    break;
                }
            }

            if (found) {
                try {
                    Class<?> beanClass = Class.forName(classInfo.name().toString(), false,
                            Thread.currentThread().getContextClassLoader());

                    //reflectionBuildItems.produce(new ReflectiveClassBuildItem(true, true, beanClass));
                    reflectionHierarchiesBuildItems.produce(new ReflectiveHierarchyBuildItem.Builder().type(convertToType(beanClass)).build());
                    elideClasses.add(beanClass);
                } catch (ClassNotFoundException e) {
                    //TODO - logging
                }
            }
        });

        synthenticBean.produce(SyntheticBeanBuildItem.configure(ClassScanner.class).scope(Singleton.class)
                .supplier(elideRecorder.createClassScanner(elideClasses))
                .unremovable()
                .addQualifier(Default.class)
                .done());

        reflectionBuildItems.produce(new ReflectiveClassBuildItem(true, true, DataSerializer.class));
        reflectionBuildItems.produce(new ReflectiveClassBuildItem(true, true, JsonApiDocument.class));
        reflectionBuildItems.produce(new ReflectiveClassBuildItem(true, true, Data.class));
        reflectionBuildItems.produce(new ReflectiveClassBuildItem(true, true, Meta.class));
        reflectionBuildItems.produce(new ReflectiveClassBuildItem(true, true, Resource.class));
        reflectionBuildItems.produce(new ReflectiveClassBuildItem(true, true, ErrorObjects.class));
        reflectionBuildItems.produce(new ReflectiveClassBuildItem(true, true, Swagger.class));
        reflectionBuildItems.produce(new ReflectiveClassBuildItem(true, true, ExternalDocs.class));
        reflectionBuildItems.produce(new ReflectiveClassBuildItem(true, true, Info.class));
        reflectionBuildItems.produce(new ReflectiveClassBuildItem(true, true, Model.class));
        reflectionBuildItems.produce(new ReflectiveClassBuildItem(true, true, Path.class));
        reflectionBuildItems.produce(new ReflectiveClassBuildItem(true, true, Response.class));
        reflectionBuildItems.produce(new ReflectiveClassBuildItem(true, true, Scheme.class));
        reflectionBuildItems.produce(new ReflectiveClassBuildItem(true, true, Tag.class));
        reflectionBuildItems.produce(new ReflectiveClassBuildItem(true, true, SecuritySchemeDefinition.class));
        reflectionBuildItems.produce(new ReflectiveClassBuildItem(true, true, Parameter.class));
    }

    private Type convertToType(Class<?> cls) {
        return Type.create(DotName.createSimple(cls.getCanonicalName()), Type.Kind.CLASS);
    }
}