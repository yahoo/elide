/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.hibernate;

import com.yahoo.elide.core.DatabaseManager;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.dbmanagers.hibernate3.HibernateManager;
import com.yahoo.elide.endpoints.AbstractApiResourceTest;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;

import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;

import java.io.IOException;

import javax.persistence.Entity;

import example.Parent;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * This class provides some helper functions for tests to start and end transactions. Any test that fails to terminate
 * its own transaction will have it rolled back at the end.
 */
public abstract class AHibernateTest extends AbstractApiResourceTest {
    protected static volatile SessionFactory sessionFactory;
    public static HibernateManager hibernateManager = null;

    /* Empty dictionary is OK provided the mapper is used for reading only */
    protected final JsonApiMapper mapper = new JsonApiMapper(new EntityDictionary());


    protected static void databaseManagerInit() {
                // method to force class initialization
        Configuration c = new Configuration();
        try {
            ClassScanner.getAnnotatedClasses(Parent.class.getPackage(), Entity.class).forEach(c::addAnnotatedClass);
        } catch (MappingException e) {
            throw new RuntimeException(e);
        }
        sessionFactory = c.configure("hibernate.cfg.xml")
                .setProperty(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread")
                .setProperty(Environment.URL,
                        "jdbc:mysql://localhost:" + System.getProperty("mysql.port", "3306") + "/root")
                .setProperty(Environment.USER, "root")
                .setProperty(Environment.PASS, "root")
                .buildSessionFactory();

        // create Example tables from beans
        SchemaExport se = new SchemaExport(c).setHaltOnError(true);
        se.drop(false, true);
        se.execute(false, true, false, true);

        if (se.getExceptions().size() != 0) {
            throw new RuntimeException("" + se.getExceptions());
        }

        hibernateManager = new HibernateManager(sessionFactory);
    }

    public static DatabaseManager getDatabaseManager() {
        if (hibernateManager == null) {
            databaseManagerInit();
        }

        return hibernateManager;
    }

    protected AHibernateTest() {
    }

    @BeforeTest
    public static void hibernateInit() {
       getDatabaseManager();
    }

    @AfterTest(alwaysRun = true)
    public static void hibernateDestroy() {
        sessionFactory.getCurrentSession().close();
        sessionFactory.close();
    }

    protected static final void startTransaction() {
        sessionFactory.getCurrentSession().beginTransaction();
    }

    protected static final void endTransaction() {
        Transaction t = sessionFactory.getCurrentSession().getTransaction();
        if (t.isActive()) {
            t.commit();
        }
    }

    protected static final void commitTransaction() {
        sessionFactory.getCurrentSession().getTransaction().commit();
    }

    protected static final void rollbackTransactionIfOpen() {
        Transaction t = sessionFactory.getCurrentSession().getTransaction();
        if (t.isActive()) {
            t.rollback();
        }
    }

    @AfterMethod(alwaysRun = true)
    public void done() {
        if (sessionFactory.getCurrentSession().isOpen()) {
            rollbackTransactionIfOpen();
        }
    }

    protected void assertEqualDocuments(String actual, String expected) {
        try {
            JsonApiDocument expectedDoc = mapper.readJsonApiDocument(expected);
            JsonApiDocument actualDoc = mapper.readJsonApiDocument(actual);
            assertEquals(actualDoc, expectedDoc, "\n" + actual + "\n" + expected + "\n");
        } catch (IOException e) {
            fail("\n" + actual + "\n" + expected + "\n", e);
        }
    }
}
