/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.extension.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import com.paiondata.elide.annotation.Include;
import com.paiondata.elide.annotation.LifeCycleHookBinding;
import com.paiondata.elide.annotation.SecurityCheck;
import com.paiondata.elide.core.security.checks.prefab.Collections;
import com.paiondata.elide.core.utils.ClassScanner;
import com.paiondata.elide.core.utils.coerce.converters.ElideTypeConverter;
import com.paiondata.elide.extension.runtime.ElideConfig;
import com.paiondata.elide.extension.runtime.ElideRecorder;
import com.paiondata.elide.extension.runtime.ElideResourceBuilder;
import com.paiondata.elide.graphql.DeferredId;
import com.paiondata.elide.graphql.GraphQLEndpoint;
import com.paiondata.elide.jsonapi.models.JsonApiDocument;
import com.paiondata.elide.jsonapi.resources.JsonApiEndpoint;
import com.paiondata.elide.jsonapi.serialization.DataDeserializer;
import com.paiondata.elide.jsonapi.serialization.DataSerializer;
import com.paiondata.elide.jsonapi.serialization.KeySerializer;
import com.paiondata.elide.jsonapi.serialization.MetaDeserializer;
import com.paiondata.elide.swagger.OpenApiDocument;
import com.paiondata.elide.swagger.resources.ApiDocsEndpoint;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.SimpleLog;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;

import graphql.schema.GraphQLSchema;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.resteasy.server.common.deployment.ResteasyDeploymentCustomizerBuildItem;
import io.quarkus.undertow.deployment.ServletInitParamBuildItem;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class ElideExtensionProcessor {
    private static final Logger LOG = Logger.getLogger(ElideExtensionProcessor.class.getName());

    private static final String FEATURE = "elide";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    public void indexDependencies(BuildProducer<IndexDependencyBuildItem> dependencies) {
        dependencies.produce(new IndexDependencyBuildItem("com.paiondata.elide", "elide-core"));
        dependencies.produce(new IndexDependencyBuildItem("io.swagger.core.v3", "swagger-core-jakarta"));
        dependencies.produce(new IndexDependencyBuildItem("io.swagger.core.v3", "swagger-models-jakarta"));

        /* Needed for CoerceUtil which loads LogFactoryImpl */
        dependencies.produce(new IndexDependencyBuildItem("commons-logging", "commons-logging"));
        dependencies.produce(new IndexDependencyBuildItem("com.graphql-java", "graphql-java"));
    }

    @BuildStep
    public AdditionalBeanBuildItem configureElideEndpoints(ElideConfig config) {
        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder();

        if (config.baseJsonapi != null) {
            LOG.info("Enabling JSON-API Endpoint");
            builder = builder.addBeanClass(JsonApiEndpoint.class);
        }

        if (config.baseGraphql != null) {
            LOG.info("Enabling GraphQL Endpoint");
            builder = builder.addBeanClass(GraphQLEndpoint.class);
        }

        if (config.baseSwagger != null && config.baseJsonapi != null) {
            LOG.info("Enabling Swagger Endpoint");
            builder = builder.addBeanClass(ApiDocsEndpoint.class);
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
                                + ApiDocsEndpoint.class.getCanonicalName()
                ));

        deploymentCustomizerProducer.produce(new ResteasyDeploymentCustomizerBuildItem((deployment) -> {
            deployment.getScannedResourceClassesWithBuilder().put(
                    ElideResourceBuilder.class.getCanonicalName(),
                    Arrays.asList(
                            JsonApiEndpoint.class.getCanonicalName(),
                            GraphQLEndpoint.class.getCanonicalName(),
                            ApiDocsEndpoint.class.getCanonicalName()
                    ));
        }));

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, false,
                ElideResourceBuilder.class.getName()));
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
                new BeanDefiningAnnotationBuildItem(
                        DotName.createSimple(Include.class.getCanonicalName())));
        additionalBeanAnnotations.add(
                new BeanDefiningAnnotationBuildItem(
                        DotName.createSimple(SecurityCheck.class.getCanonicalName())));
        additionalBeanAnnotations.add(
                new BeanDefiningAnnotationBuildItem(
                        DotName.createSimple(LifeCycleHookBinding.class.getCanonicalName())));
        additionalBeanAnnotations.add(
                new BeanDefiningAnnotationBuildItem(
                        DotName.createSimple(ElideTypeConverter.class.getCanonicalName())));

        return additionalBeanAnnotations;
    }

    @BuildStep
    @Record(STATIC_INIT)
    public void configureElideModels(
            CombinedIndexBuildItem index,
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

                    reflectionHierarchiesBuildItems.produce(new ReflectiveHierarchyBuildItem.Builder()
                            .type(convertToType(beanClass))
                            .build());
                    elideClasses.add(beanClass);
                } catch (ClassNotFoundException e) {
                    LOG.error("Unable to load class from Jandex Index: " + classInfo.name());
                }
            }

        });

        synthenticBean.produce(SyntheticBeanBuildItem.configure(ClassScanner.class).scope(Singleton.class)
                .supplier(elideRecorder.createClassScanner(elideClasses))
                .unremovable()
                .addQualifier(Default.class)
                .done());

        reflectionHierarchiesBuildItems.produce(new ReflectiveHierarchyBuildItem.Builder()
                .type(convertToType(JsonApiDocument.class))
                .build());
        reflectionHierarchiesBuildItems.produce(new ReflectiveHierarchyBuildItem.Builder()
                .type(convertToType(OpenApiDocument.class))
                .build());
        reflectionHierarchiesBuildItems.produce(new ReflectiveHierarchyBuildItem.Builder()
                .type(convertToType(GraphQLSchema.class))
                .build());

        //JSON-API Serialization Classes:
        reflectionBuildItems.produce(new ReflectiveClassBuildItem(true, true, DataSerializer.class));
        reflectionBuildItems.produce(new ReflectiveClassBuildItem(true, true, DataDeserializer.class));
        reflectionBuildItems.produce(new ReflectiveClassBuildItem(true, true, MetaDeserializer.class));
        reflectionBuildItems.produce(new ReflectiveClassBuildItem(true, true, KeySerializer.class));

        //Prefabbed Checks:
        reflectionBuildItems.produce(new ReflectiveClassBuildItem(true, true, Collections.AppendOnly.class));
        reflectionBuildItems.produce(new ReflectiveClassBuildItem(true, true, Collections.RemoveOnly.class));

        //GraphQL Schema:
        reflectionBuildItems.produce(new ReflectiveClassBuildItem(true, true, DeferredId.class));
//        reflectionBuildItems.produce(new ReflectiveClassBuildItem(true, true, DeferredId.SerializeId.class));

        //Needed by elide dependency coerce utils which pulls in commons logging.
//        reflectionBuildItems.produce(new ReflectiveClassBuildItem(true, true, JBossLogFactory.class));
        reflectionBuildItems.produce(new ReflectiveClassBuildItem(true, true, LogFactory.class));
        reflectionBuildItems.produce(new ReflectiveClassBuildItem(true, true, SimpleLog.class));
    }

    private Type convertToType(Class<?> cls) {
        return Type.create(DotName.createSimple(cls.getCanonicalName()), Type.Kind.CLASS);
    }
}
