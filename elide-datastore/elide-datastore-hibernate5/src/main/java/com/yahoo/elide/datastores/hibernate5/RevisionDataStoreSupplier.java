package com.yahoo.elide.datastores.hibernate5;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.utils.ClassScanner;
import org.hibernate.MappingException;
import org.hibernate.ejb.HibernateEntityManager;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.rmi.CORBA.Util;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Supplier;

public class RevisionDataStoreSupplier implements Supplier<DataStore> {

    private String packageName;
    public RevisionDataStoreSupplier(String packageName) {
        this.packageName = packageName;
    }
    @Override
    public DataStore get() {
        Map<String, Object> options = new HashMap<>();
        ArrayList<Class> bindClasses = new ArrayList<>();

        try {
            bindClasses.addAll(getAllEntities(packageName));
        } catch (MappingException e) {
            throw new IllegalStateException(e);
        }

        options.put("javax.persistence.jdbc.driver", "com.mysql.jdbc.Driver");
        options.put("javax.persistence.jdbc.url", "jdbc:mysql://localhost:3306/test?serverTimezone=UTC");
        options.put("javax.persistence.jdbc.user", "root");
        options.put("javax.persistence.jdbc.password", "root");
        //options.put("hibernate.ejb.loaded.classes", bindClasses);
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("elide-tests", options);
        HibernateEntityManager em = (HibernateEntityManager) emf.createEntityManager();
        return new HibernateRevisionsDataStore(em);
    }

    private static HashSet<Class> getAllEntities(String packageName) {
        return new HashSet<>(new Reflections(new ConfigurationBuilder()
                .setScanners(new SubTypesScanner(false), new TypeAnnotationsScanner())
                .setUrls(ClasspathHelper.forClassLoader(ClassLoader.getSystemClassLoader()))
                .filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix(packageName))))
                .getTypesAnnotatedWith(Entity.class));
    }
}
