package com.yahoo.elide.datastores.hibernate5;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.utils.ClassScanner;
import example.Parent;
import org.hibernate.MappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import java.util.function.Supplier;

public class RevisionDataStoreSupplier implements Supplier<DataStore> {

    @Override
    public DataStore get() {
        MetadataSources metadataSources = new MetadataSources(
                new StandardServiceRegistryBuilder()
                        .configure("hibernate.cfg.xml")
                        .applySetting(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread")
                        .applySetting(Environment.URL, "jdbc:mysql://localhost:"
                                + System.getProperty("mysql.port", "3306")
                                + "/root?serverTimezone=UTC")
                        .applySetting("hibernate.hbm2ddl.auto", "create-drop")
                        .applySetting(Environment.USER, "root")
                        .applySetting(Environment.PASS, "root")
                        .build());

        try {
            ClassScanner.getAnnotatedClasses(Parent.class.getPackage(), Entity.class)
                    .stream().filter(c -> c.isAnnotationPresent(Audited.class))
                    .forEach(metadataSources::addAnnotatedClass);
        } catch (MappingException e) {
            throw new RuntimeException(e);
        }

        MetadataImplementor metadataImplementor = (MetadataImplementor) metadataSources.buildMetadata();
        return new HibernateRevisionsDataStore(metadataImplementor.buildSessionFactory());
    }
}
