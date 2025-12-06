/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.extension.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import static io.quarkus.gizmo.Type.classType;
import static io.quarkus.gizmo.Type.parameterizedType;
import static io.quarkus.gizmo.Type.voidType;

import com.yahoo.elide.Elide;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.LifeCycleHookBinding;
import com.yahoo.elide.annotation.SecurityCheck;
import com.yahoo.elide.core.dictionary.RelationshipType;
import com.yahoo.elide.core.request.route.RouteResolver;
import com.yahoo.elide.core.security.checks.OperationCheck;
import com.yahoo.elide.core.security.checks.prefab.Collections;
import com.yahoo.elide.core.utils.ClassScanner;
import com.yahoo.elide.core.utils.coerce.converters.ElideTypeConverter;
import com.yahoo.elide.extension.runtime.ElideBeans;
import com.yahoo.elide.extension.runtime.ElideConfig;
import com.yahoo.elide.extension.runtime.ElideRecorder;
import com.yahoo.elide.graphql.DeferredId;
import com.yahoo.elide.graphql.GraphQLEndpoint;
import com.yahoo.elide.jsonapi.models.*;
import com.yahoo.elide.jsonapi.resources.JsonApiEndpoint;
import com.yahoo.elide.jsonapi.serialization.DataDeserializer;
import com.yahoo.elide.jsonapi.serialization.DataSerializer;
import com.yahoo.elide.jsonapi.serialization.KeySerializer;
import com.yahoo.elide.jsonapi.serialization.MetaDeserializer;
import com.yahoo.elide.swagger.OpenApiDocument;
import com.yahoo.elide.swagger.resources.ApiDocsEndpoint;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.SimpleLog;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import graphql.execution.DataFetcherExceptionHandler;
import graphql.schema.GraphQLSchema;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.SignatureBuilder;
import io.quarkus.resteasy.reactive.spi.GeneratedJaxRsResourceBuildItem;
import io.quarkus.resteasy.reactive.spi.GeneratedJaxRsResourceGizmoAdaptor;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Path;

import java.util.*;

/**
 * Quarkus extension processor for Elide.
 */
public class ElideExtensionProcessor {
    private static final Logger LOG = Logger.getLogger(ElideExtensionProcessor.class.getName());

    private static final String FEATURE = "elide";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    public void indexDependencies(BuildProducer<IndexDependencyBuildItem> dependencies) {
        dependencies.produce(new IndexDependencyBuildItem("io.swagger.core.v3", "swagger-core-jakarta"));
        dependencies.produce(new IndexDependencyBuildItem("io.swagger.core.v3", "swagger-models-jakarta"));

        /* Needed for CoerceUtil which loads LogFactoryImpl */
        dependencies.produce(new IndexDependencyBuildItem("commons-logging", "commons-logging"));
        dependencies.produce(new IndexDependencyBuildItem("com.graphql-java", "graphql-java"));
    }

    /**
     * When Quarkus warns during build-time about Elide-specific classes that "are not in the Jandex  index"
     * we add those classes here. Unlike using the IndexDependencyBuildItem, this more specific approach
     * prevents the Elide JAX-RS endpoints from being deployed at their default "/" paths.
     * @param additionalIndexedClassesBuildItemBuildProducer
     */
    @BuildStep
    public void indexElideClasses(BuildProducer<AdditionalIndexedClassesBuildItem>
                                                 additionalIndexedClassesBuildItemBuildProducer) {
        additionalIndexedClassesBuildItemBuildProducer.produce(
                new io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem(
                        OperationCheck.class.getCanonicalName(),
                        RelationshipType.class.getCanonicalName(),
                        JsonApiDocument.class.getCanonicalName(),
                        Data.class.getCanonicalName(),
                        Meta.class.getCanonicalName(),
                        Relationship.class.getCanonicalName(),
                        Resource.class.getCanonicalName(),
                        ResourceIdentifier.class.getCanonicalName(),
                        OpenApiDocument.class.getCanonicalName()
                ));
    }

    @BuildStep
    public void registerElideBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(ElideBeans.class)
                .build());
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(ElideConfig.class)
                .build());
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(ElideRecorder.class)
                .build());
    }

    @BuildStep
    public void configureElideEndpoints(ElideConfig config,
            BuildProducer<GeneratedJaxRsResourceBuildItem> generatedJaxRsResourceBuildItemBuildProducer) {
        if (config.jsonApiPath != null) {
            LOG.infof("Enabling JSON-API Endpoint for path: %s", config.jsonApiPath);
            generateEndpointClass(generatedJaxRsResourceBuildItemBuildProducer, "JsonApi",
                    JsonApiEndpoint.class, config.jsonApiPath,
                    new Param("elide", Elide.class, null), new Param(Optional.class, RouteResolver.class));
        }

        if (config.graphqlPath != null) {
            LOG.infof("Enabling GraphQL Endpoint for path: %s", config.graphqlPath);
            generateEndpointClass(generatedJaxRsResourceBuildItemBuildProducer, "GraphQL",
                    GraphQLEndpoint.class, config.graphqlPath,
                    new Param("elide", Elide.class, null),
                    new Param(Optional.class, DataFetcherExceptionHandler.class),
                    new Param(Optional.class, RouteResolver.class));
        }

        if (config.apiDocsPath != null && config.jsonApiPath != null) {
            LOG.infof("Enabling Swagger Endpoint for path: %s", config.apiDocsPath);
            generateEndpointClass(generatedJaxRsResourceBuildItemBuildProducer, "ApiDocs",
                    ApiDocsEndpoint.class, config.apiDocsPath,
                    new Param("apiDocs", List.class, ApiDocsEndpoint.ApiDocsRegistration.class),
                    new Param("elide", Elide.class, null),
                    new Param(Optional.class, RouteResolver.class));
        }
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

    private org.jboss.jandex.Type convertToType(Class<?> cls) {
        return org.jboss.jandex.Type.create(DotName.createSimple(cls.getCanonicalName()), Type.Kind.CLASS);
    }

    /**
     * We customise @Path.value at build time by generating a subclass of the corresponding Elide endpoint with Gizmo.
     * Note that the ability to customise the value of @Path.value depends on us NOT including the standard Elide
     * endpoint classes by default. If they were present (for example, if they were added to the indexed classes),
     * Quarkus would register them with their default @Path values (in addition to these generated instances).
     *
     * @param generatedJaxRsResourceBuildItemBuildProducer the build producer
     * @param endpointStyle the endpoint style name
     * @param endpointClass the endpoint class
     * @param customPath the custom path
     * @param params the constructor parameters
     */
    private void generateEndpointClass(
            BuildProducer<GeneratedJaxRsResourceBuildItem> generatedJaxRsResourceBuildItemBuildProducer,
            String endpointStyle, Class endpointClass, String customPath, Param... params) {
        // Deconstruct Param instances
        Class[] paramClasses = new Class[params.length];
        io.quarkus.gizmo.Type[] paramTypes = new io.quarkus.gizmo.Type[params.length];
        String[] paramNames = new String[params.length];
        for (int i = 0; i < params.length; i++) {
            paramClasses[i] = params[i].clazz;
            paramNames[i] = params[i].atNamed;
            if (params[i].parameterizedType != null) {
                paramTypes[i] = parameterizedType(classType(params[i].clazz),
                        classType(params[i].parameterizedType));
            } else {
                paramTypes[i] = classType(params[i].clazz);
            }
        }
        // Use Gizmo to generate an instance of the endpoint class with a custom @Path(value)
        String generatedClassName = "com.yahoo.elide.extension.generated.Configurable"
                + endpointStyle + "Endpoint";
        ClassOutput classOutput = new GeneratedJaxRsResourceGizmoAdaptor(
                generatedJaxRsResourceBuildItemBuildProducer);
        try (ClassCreator classCreator = ClassCreator.builder()
                .classOutput(classOutput)
                .className(generatedClassName)
                .superClass(endpointClass)
                .build()) {

            // Add @Path annotation with custom path value
            classCreator.addAnnotation(Path.class).addValue("value", customPath);

            // Create a constructor that matches endpoint's current @Injected constructor
            try (MethodCreator constructor = classCreator.getMethodCreator("<init>", void.class,
                    paramClasses)) {
                // Using Gizmo's SignatureBuilder allows us to define a parameterised injection point
                SignatureBuilder.MethodSignatureBuilder signatureBuilder = SignatureBuilder.forMethod()
                        .setReturnType(voidType());
                Arrays.stream(paramTypes).forEach(signatureBuilder::addParameterType);
                constructor.setSignature(signatureBuilder.build());
                // Add @Inject annotation to constructor
                constructor.addAnnotation(jakarta.inject.Inject.class);
                // Add @Named annotations to parameters where applicable
                Arrays.stream(paramNames).forEach((paramName) -> {
                    if (paramName != null) {
                        int paramIndex = Arrays.asList(paramNames).indexOf(paramName);
                        constructor.getParameterAnnotations(paramIndex).addAnnotation(Named.class)
                                .addValue("value", paramName);
                    }
                });
                // Call super constructor with the parameters
                ResultHandle[] constructorParams = new ResultHandle[constructor.getMethodDescriptor()
                        .getParameterTypes().length];
                for (int i = 0; i < constructorParams.length; i++) {
                    constructorParams[i] = constructor.getMethodParam(i);
                }
                constructor.invokeSpecialMethod(
                        MethodDescriptor.ofConstructor(endpointClass, paramClasses),
                        constructor.getThis(), constructorParams);
                constructor.returnValue(null);
            }
        }
        LOG.infof("Generated configurable %s endpoint with path: %s", endpointStyle, customPath);
    }

    class Param {
        String atNamed;
        Class<?> clazz;
        Class<?> parameterizedType;

        public Param(String atNamed, Class<?> clazz, Class<?> parameterizedType) {
            this.atNamed = atNamed;
            this.clazz = clazz;
            this.parameterizedType = parameterizedType;
        }

        public Param(Class<?> clazz, Class<?> parameterizedType) {
            this.clazz = clazz;
            this.parameterizedType = parameterizedType;
        }
    }
}
