package com.yahoo.elide.datastores.hibernate5;

import com.google.common.base.Preconditions;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import org.hibernate.ScrollMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.jpa.HibernateEntityManager;
import org.hibernate.metadata.ClassMetadata;

import javax.persistence.Entity;
import javax.persistence.EntityManager;

public class HibernateRevisionsDataStore extends HibernateSessionFactoryStore {


    public HibernateRevisionsDataStore(SessionFactory sessionFactory) {
        super(sessionFactory, false, ScrollMode.SCROLL_SENSITIVE);
    }

    @Override
    @SuppressWarnings("resource")
    public DataStoreTransaction beginTransaction() {
        Session session = sessionFactory.getCurrentSession();
        Preconditions.checkNotNull(session);
        session.beginTransaction();
        return new HibernateRevisionsTransaction(AuditReaderFactory.get(session), session);
    }
}
