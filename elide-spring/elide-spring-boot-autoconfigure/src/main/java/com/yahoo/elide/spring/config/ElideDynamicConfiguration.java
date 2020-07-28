/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.config;

import com.yahoo.elide.contrib.dynamicconfighelpers.compile.ElideDynamicEntityCompiler;
import com.yahoo.elide.datastores.jpa.PersistenceUnitInfoImpl;
import com.yahoo.elide.utils.ClassScanner;

import org.hibernate.cfg.AvailableSettings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateProperties;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateProperties.Naming;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitManager;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.persistence.Entity;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.sql.DataSource;

/**
 * Dynamic Configuration For Elide Services. Override any of the beans (by
 * defining your own) and setting flags to disable in properties to change the
 * default behavior.
 */

@Slf4j
@Configuration
@EnableConfigurationProperties(ElideConfigProperties.class)
@ConditionalOnExpression("${elide.aggregation-store.enabled:false} and ${elide.dynamic-config.enabled:false}")
public class ElideDynamicConfiguration {

    public static final String HIBERNATE_DDL_AUTO = "hibernate.hbm2ddl.auto";
    public static final String HIBERNATE_PHYSICAL_NAMING = "hibernate.physical_naming_strategy";
    public static final String HIBERNATE_IMPLICIT_NAMING = "hibernate.implicit_naming_strategy";
    public static final String HIBERNATE_ID_GEN_MAPPING = "hibernate.use-new-id-generator-mappings";

    /**
     * Configure factory bean to create EntityManagerFactory for Dynamic Configuration.
     * @param source :DataSource for JPA
     * @param jpaProperties : JPA Config Properties
     * @param hibernateProperties : Hibernate Config Properties
     * @param dynamicCompiler : ElideDynamicEntityCompiler
     * @return LocalContainerEntityManagerFactoryBean bean
     */
    @Bean
    @ConditionalOnMissingBean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory (
            DataSource source,
            JpaProperties jpaProperties,
            HibernateProperties hibernateProperties,
            ObjectProvider<ElideDynamicEntityCompiler> dynamicCompiler) {

            //Map for Persistent Unit properties
            Map<String, Object> puiPropertyMap = new HashMap<>();

            //Bind entity classes from classpath to Persistence Unit
            ArrayList<Class> bindClasses = new ArrayList<>();
            bindClasses.addAll(ClassScanner.getAnnotatedClasses(Entity.class));

            //Map of JPA Properties to be be passed to EntityManager
            Map<String, String> jpaPropMap = jpaProperties.getProperties();

            String hibernateGetDDLAuto = hibernateProperties.getDdlAuto();
            Naming hibernateGetNaming =  hibernateProperties.getNaming();
            String hibernateImplicitStrategy = hibernateGetNaming.getImplicitStrategy();
            String hibernatePhysicalStrategy = hibernateGetNaming.getPhysicalStrategy();
            Boolean hibernateGetIdenGen = hibernateProperties.isUseNewIdGeneratorMappings();

            //Set the relevant property in JPA corresponding to Hibernate Property Value
            hibernateJPAPropertyOverride(jpaPropMap, HIBERNATE_DDL_AUTO, hibernateGetDDLAuto);
            hibernateJPAPropertyOverride(jpaPropMap, HIBERNATE_PHYSICAL_NAMING, hibernatePhysicalStrategy);
            hibernateJPAPropertyOverride(jpaPropMap, HIBERNATE_IMPLICIT_NAMING, hibernateImplicitStrategy);
            hibernateJPAPropertyOverride(jpaPropMap, HIBERNATE_ID_GEN_MAPPING,
                    (hibernateGetIdenGen != null) ? hibernateGetIdenGen.toString() : null);

            ElideDynamicEntityCompiler compiler = dynamicCompiler.getIfAvailable();

            Collection<ClassLoader> classLoaders = new ArrayList<>();
            classLoaders.add(compiler.getClassLoader());

            //Add dynamic classes to Pui Map
            puiPropertyMap.put(AvailableSettings.CLASSLOADERS, classLoaders);
            //Add classpath entity model classes to Pui Map
            puiPropertyMap.put(AvailableSettings.LOADED_CLASSES, bindClasses);

            //pui properties from pui map
            Properties puiProps = new Properties();
            puiProps.putAll(puiPropertyMap);

            //Create Elide dynamic Persistence Unit
            PersistenceUnitInfoImpl elideDynamicPersistenceUnit =
                    new PersistenceUnitInfoImpl("dynamic", compiler.classNames, puiProps,
                    compiler.getClassLoader());
            elideDynamicPersistenceUnit.setNonJtaDataSource(source);
            elideDynamicPersistenceUnit.setJtaDataSource(source);

            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            vendorAdapter.setShowSql(jpaProperties.isShowSql());
            vendorAdapter.setGenerateDdl(jpaProperties.isGenerateDdl());
            if (jpaProperties.getDatabase() != null) {
                vendorAdapter.setDatabase(jpaProperties.getDatabase());
            }
            if (jpaProperties.getDatabasePlatform() != null) {
                vendorAdapter.setDatabasePlatform(jpaProperties.getDatabasePlatform());
            }

            LocalContainerEntityManagerFactoryBean bean = new LocalContainerEntityManagerFactoryBean();
            bean.setJpaVendorAdapter(vendorAdapter);

            //Add JPA Properties from Application.yaml
            bean.setJpaPropertyMap(jpaPropMap);

            //Add Classes
            bean.setJpaPropertyMap(puiPropertyMap);

            bean.setPersistenceUnitManager(new PersistenceUnitManager() {
                @Override
                public PersistenceUnitInfo obtainDefaultPersistenceUnitInfo() throws IllegalStateException {
                    return elideDynamicPersistenceUnit;
                }

                @Override
                public PersistenceUnitInfo obtainPersistenceUnitInfo(String persistenceUnitName)
                        throws IllegalArgumentException, IllegalStateException {
                    return elideDynamicPersistenceUnit;
                }
            });

            return bean;
    }

    /**
     * Override Hibernate properties in application.yaml with jpa hibernate properties.
     */
    private void hibernateJPAPropertyOverride(Map<String, String> jpaPropMap,
        String jpaPropertyName, String hibernateProperty) {
        if (jpaPropMap.get(jpaPropertyName) == null && hibernateProperty != null) {
            jpaPropMap.put(jpaPropertyName, hibernateProperty);
        }

    }
}
