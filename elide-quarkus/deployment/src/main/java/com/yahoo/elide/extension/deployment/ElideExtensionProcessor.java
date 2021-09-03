package com.yahoo.elide.extension.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import com.yahoo.elide.core.utils.ClassScanner;
import com.yahoo.elide.extension.runtime.ElideRecorder;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyIgnoreWarningBuildItem;
import io.quarkus.hibernate.orm.deployment.JpaModelIndexBuildItem;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Singleton;

class ElideExtensionProcessor {

    private static final String FEATURE = "elide-extension";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    @Record(STATIC_INIT)
    public List<ReflectiveHierarchyIgnoreWarningBuildItem> elideModels(
            JpaModelIndexBuildItem index,
            ElideRecorder elideRecorder,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<SyntheticBeanBuildItem> synthenticBean
    ) {
        List<ReflectiveHierarchyIgnoreWarningBuildItem> reflectionBuildItems = new ArrayList<>();
        List<Class<?>> elideClasses = new ArrayList<>();

        index.getIndex().getKnownClasses().forEach(classInfo -> {
            AnnotationInstance instance =
                    classInfo.classAnnotation(DotName.createSimple("com.yahoo.elide.annotation.Include"));

            if (instance != null) {

                try {
                    Class<?> beanClass = Class.forName(classInfo.name().toString(), false,
                            Thread.currentThread().getContextClassLoader());

                    elideClasses.add(beanClass);
                } catch (ClassNotFoundException e) {
                    //TODO - logging
                }
                reflectionBuildItems.add(new ReflectiveHierarchyIgnoreWarningBuildItem(classInfo.name()));
            }

            additionalBeans.produce(AdditionalBeanBuildItem.builder().addBeanClass(ClassScanner.class).build());

            synthenticBean.produce(SyntheticBeanBuildItem.configure(ClassScanner.class).scope(Singleton.class)
                    .runtimeValue(elideRecorder.createClassScanner(elideClasses))
                    .done());
        });
        return reflectionBuildItems;
    }
}