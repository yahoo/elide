/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.standalone;

import com.yahoo.elide.utils.ClassScanner;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;

import org.mdkt.compiler.InMemoryJavaCompiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitInfo;

/**
 * Util.
 */
public class Util {

    public static EntityManagerFactory getEntityManagerFactory(String modelPackageName, Properties options) {

        // Configure default options for example service
        if (options.isEmpty()) {
            options.put("hibernate.show_sql", "true");
            options.put("hibernate.hbm2ddl.auto", "create");
            options.put("hibernate.dialect", "org.hibernate.dialect.MySQL5Dialect");
            options.put("hibernate.current_session_context_class", "thread");
            options.put("hibernate.jdbc.use_scrollable_resultset", "true");

            // Collection Proxy & JDBC Batching
            options.put("hibernate.jdbc.batch_size", "50");
            options.put("hibernate.jdbc.fetch_size", "50");
            options.put("hibernate.default_batch_fetch_size", "100");

            // Hikari Connection Pool Settings
            options.putIfAbsent("hibernate.connection.provider_class",
                    "com.zaxxer.hikari.hibernate.HikariConnectionProvider");
            options.putIfAbsent("hibernate.hikari.connectionTimeout", "20000");
            options.putIfAbsent("hibernate.hikari.maximumPoolSize", "30");
            options.putIfAbsent("hibernate.hikari.idleTimeout", "30000");

            options.put("javax.persistence.jdbc.driver", "com.mysql.jdbc.Driver");
            options.put("javax.persistence.jdbc.url", "jdbc:mysql://localhost/elide?serverTimezone=UTC");
            options.put("javax.persistence.jdbc.user", "elide");
            options.put("javax.persistence.jdbc.password", "elide123");
        }

        InMemoryJavaCompiler compiler = InMemoryJavaCompiler.newInstance().useParentClassLoader(new MyClassLoader(ClassLoader.getSystemClassLoader()));

        String post = "package com.yahoo.elide.standalone.models;\n" +
                "\n" +
                "import com.yahoo.elide.annotation.CreatePermission;\n" +
                "import com.yahoo.elide.annotation.Include;\n" +
                "import com.yahoo.elide.annotation.UpdatePermission;\n" +
                "import com.yahoo.elide.standalone.checks.AdminCheck;\n" +
                "import lombok.Data;\n" +
                "\n" +
                "import java.util.Date;\n" +
                "\n" +
                "import javax.persistence.Column;\n" +
                "import javax.persistence.Entity;\n" +
                "import javax.persistence.Id;\n" +
                "import javax.persistence.Temporal;\n" +
                "import javax.persistence.TemporalType;\n" +
                "\n" +
                "@Entity\n" +
                "@Include(rootLevel = true)\n" +
                "@Data\n" +
                "public class Post {\n" +
                "    @Id\n" +
                "    private long id;\n" +
                "\n" +
                "    @Column(nullable = false)\n" +
                "    private String content;\n" +
                "\n" +
                "    @Temporal( TemporalType.TIMESTAMP )\n" +
                "    private Date date;\n" +
                "\n" +
                "    @CreatePermission(expression = AdminCheck.USER_IS_ADMIN)\n" +
                "    @UpdatePermission(expression = AdminCheck.USER_IS_ADMIN)\n" +
                "    private boolean abusiveContent;\n" +
                "}";

        try {
            Class<?> foo = compiler.compile("com.yahoo.elide.standalone.models.Post", post);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        Collection<ClassLoader> classLoaders = new ArrayList<>();
        classLoaders.add(compiler.getClassloader());

        options.put(AvailableSettings.CLASSLOADERS, classLoaders);

        PersistenceUnitInfo persistenceUnitInfo = new PersistenceUnitInfoImpl("elide-stand-alone",
                getAllEntities(modelPackageName), options, compiler.getClassloader());

        EntityManagerFactory impl = new EntityManagerFactoryBuilderImpl(
                new PersistenceUnitInfoDescriptor(persistenceUnitInfo), new HashMap<>(), compiler.getClassloader())
                .build();

        return impl;
    }

    /**
     * Get all the entities in a package.
     *
     * @param packageName Package name
     * @return All entities found in package.
     */
    public static List<String> getAllEntities(String packageName) {
        return Arrays.asList("com.yahoo.elide.standalone.models.Post");
    }
}
